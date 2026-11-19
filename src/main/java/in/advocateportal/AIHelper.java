package in.advocateportal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * AI legal guide.
 *
 * Every message can be answered in up to three ways, and they work together or on their own:
 *  1. Keyword matching detects the practice area (property, divorce, cheque bounce ...), lists the
 *     documents to gather and suggests registered advocates who handle that kind of case.
 *     It needs no API key.
 *  2. Gemini (when GEMINI_API_KEY or gemini.api.key is configured) writes a plain-language explanation.
 *     It works with keywords and without them: when no keyword matches, Gemini answers the question
 *     directly and also names the practice area, so advocate suggestions still appear.
 *  3. When nothing can be matched, the guide returns one-tap keyword suggestions the client can add.
 *
 * The chat continues across questions: the conversation is kept in the HTTP session and sent to Gemini
 * with every follow-up, so "what documents do I need?" is answered in the context of the earlier messages.
 * Opening the AI legal guide page (or posting reset=1) starts a fresh conversation.
 */
@WebServlet("/ai-assist")
public class AIHelper extends HttpServlet {

    private static final String DISCLAIMER =
            "This is general information, not legal advice. Please confirm the details with a qualified advocate.";

    private static final String NO_MATCH =
            "I could not match this to a specific area of law yet. Tap a keyword below to add it to your question, "
            + "or describe your situation in a little more detail: who is involved, what happened and what you want to achieve.";

    /** Session attribute names: the Gemini conversation, and the practice areas of the current topic. */
    static final String HISTORY = "aiHistory";
    static final String AREAS = "aiAreas";

    /** Messages (questions and answers together) remembered per conversation. */
    private static final int MAX_HISTORY = 12;
    private static final int MAX_QUESTION = 2000;
    private static final int MAX_ANSWER = 4000;

    /** The optional first line Gemini is asked to write: "AREA: Property Law | Civil Law". */
    private static final Pattern AREA_LINE = Pattern.compile("^[\\s*#>_`]*AREA\\s*:\\s*([^\\r\\n]*)", Pattern.CASE_INSENSITIVE);

    private static final class Match {

        int id, years, score, reviews, fee;
        double rating;
        String name, spec, bio, city, photo;
        boolean general;
    }

    private static final class Gem {

        String text, error;
        List<LegalCategories.Cat> areas = new ArrayList<>();
    }

    /** One message of the conversation. Serializable so Tomcat can keep it when sessions are persisted. */
    private static final class Turn implements Serializable {

        private static final long serialVersionUID = 1L;
        final String role;
        final String text;

        Turn(String role, String text) {
            this.role = role;
            this.text = text;
        }
    }

    @Override
    protected void doPost(HttpServletRequest q, HttpServletResponse p) throws IOException {
        q.setCharacterEncoding("UTF-8");
        p.setContentType("application/json;charset=UTF-8");
        p.setHeader("Cache-Control", "no-store");

        HttpSession s = q.getSession(false);
        if (s == null || s.getAttribute("uid") == null) {
            p.setStatus(401);
            p.getWriter().write(plainReply("Please sign in first."));
            return;
        }
        if ("1".equals(q.getParameter("reset"))) {
            clearConversation(s);
            p.getWriter().write(plainReply("Started a new conversation."));
            return;
        }
        String question = q.getParameter("question");
        question = question == null ? "" : question.trim();
        if (question.isEmpty()) {
            p.getWriter().write(plainReply("Enter a question to get started."));
            return;
        }
        if (question.length() > MAX_QUESTION) {
            question = question.substring(0, MAX_QUESTION);
        }

        // 1. keyword matching: always available
        List<LegalCategories.Cat> cats = LegalCategories.detect(question, 2);
        boolean keywordMatch = !cats.isEmpty();
        List<Turn> history = history(s);
        List<String> previousAreas = previousAreas(s);
        boolean followUp = !history.isEmpty();

        // 2. Gemini: optional, with or without keywords, and aware of the earlier messages
        String key = Config.get("GEMINI_API_KEY", "gemini.api.key", "");
        Gem gem = key.isEmpty() ? null : gemini(key, question, cats, history);
        boolean geminiAnswered = gem != null && gem.text != null;
        String geminiState = "off";
        String geminiMessage = "Gemini is not configured. Add your API key in application.properties "
                + "to get answers to any legal question, with or without keywords.";
        if (geminiAnswered) {
            geminiState = "ok";
            geminiMessage = "Legal explanation generated by Gemini.";
        } else if (gem != null) {
            geminiState = "error";
            geminiMessage = gem.error;
        }

        // no keyword matched: use the practice area Gemini identified, so advocates can still be suggested
        if (cats.isEmpty() && gem != null && !gem.areas.isEmpty()) {
            cats = gem.areas;
        }

        // The checklist and advocates are shown for a new topic, or whenever Gemini is not answering.
        // A follow-up on the same topic gets just the answer, so the chat does not repeat itself.
        boolean showDetails = !cats.isEmpty() && (!geminiAnswered || !sameAreas(cats, previousAreas));

        List<Match> advocates = new ArrayList<>();
        if (showDetails) {
            try {
                advocates = findAdvocates(cats);
            } catch (Exception e) {
                // database problem: still return the guidance text
            }
        }

        StringBuilder reply = new StringBuilder();
        if (showDetails) {
            reply.append("This looks like a ").append(labels(cats, " and ")).append(" matter.\n");
            if (geminiAnswered) {
                reply.append("\nLegal explanation (Gemini):\n").append(gem.text).append("\n");
            }
            for (LegalCategories.Cat c : cats) {
                reply.append("\n").append(c.label).append(" - ").append(c.summary).append("\n");
                reply.append("Documents to gather:\n");
                for (String d : c.documents) {
                    reply.append(" - ").append(d).append("\n");
                }
                reply.append("Questions to ask the advocate:\n");
                for (String qs : c.questions) {
                    reply.append(" - ").append(qs).append("\n");
                }
            }
            if (advocates.isEmpty()) {
                reply.append("\nNo advocate has listed this practice area yet. You can browse everyone on the Find advocates page.\n");
            } else if (advocates.get(0).general) {
                reply.append("\nNo advocate has listed this practice area yet, so these general-practice advocates may help.\n");
            } else {
                reply.append("\nAdvocates who handle this kind of case are listed below.\n");
            }
            reply.append("\n").append(DISCLAIMER);
        } else if (geminiAnswered) {
            reply.append(gem.text);
            if (!followUp) {
                reply.append("\n\n").append(DISCLAIMER);
            }
        } else {
            reply.append(NO_MATCH);
        }

        // one-tap keyword suggestions
        List<String> suggestions = new ArrayList<>();
        String suggestTitle = "";
        if (cats.isEmpty() && !(geminiAnswered && followUp)) {
            suggestions = LegalCategories.suggestKeywords(question, cats, 8);
            suggestTitle = geminiAnswered
                    ? "Add a keyword to also get a document checklist and advocate suggestions:"
                    : "Tap a keyword to add it to your question:";
        } else if (showDetails) {
            suggestions = LegalCategories.suggestKeywords(question, cats, 6);
            suggestTitle = "Add more keywords to refine your question and find closer matches:";
        }

        if (geminiAnswered) {
            remember(s, question, gem.text);
        }
        if (!cats.isEmpty()) {
            rememberAreas(s, cats);
        }

        String mode = keywordMatch ? (geminiAnswered ? "keywords+gemini" : "keywords") : (geminiAnswered ? "gemini" : "none");
        p.getWriter().write(toJson(reply.toString(), cats, geminiState, geminiMessage, mode, followUp, advocates, suggestions, suggestTitle));
    }

    /* ---------- JSON output ---------- */

    /** A reply that carries only a message (sign-in prompt, empty question, new conversation). */
    private static String plainReply(String message) {
        return toJson(message, new ArrayList<LegalCategories.Cat>(), "off", "", "none", false,
                new ArrayList<Match>(), new ArrayList<String>(), "");
    }

    private static String toJson(String reply, List<LegalCategories.Cat> cats, String geminiState, String geminiMessage,
            String mode, boolean followUp, List<Match> advocates, List<String> suggestions, String suggestTitle) {
        StringBuilder out = new StringBuilder();
        out.append("{\"reply\":\"").append(Util.json(reply)).append("\",\"categories\":[");
        for (int i = 0; i < cats.size(); i++) {
            out.append(i > 0 ? "," : "").append("\"").append(Util.json(cats.get(i).label)).append("\"");
        }
        out.append("],\"gemini\":\"").append(geminiState)
                .append("\",\"geminiMessage\":\"").append(Util.json(geminiMessage))
                .append("\",\"mode\":\"").append(mode)
                .append("\",\"followUp\":").append(followUp)
                .append(",\"suggestTitle\":\"").append(Util.json(suggestTitle))
                .append("\",\"suggestions\":[");
        for (int i = 0; i < suggestions.size(); i++) {
            out.append(i > 0 ? "," : "").append("\"").append(Util.json(suggestions.get(i))).append("\"");
        }
        out.append("],\"advocates\":[");
        for (int i = 0; i < advocates.size(); i++) {
            Match m = advocates.get(i);
            String bio = m.bio == null ? "" : m.bio;
            if (bio.length() > 140) {
                bio = bio.substring(0, 140) + "...";
            }
            out.append(i > 0 ? "," : "").append("{\"id\":").append(m.id)
                    .append(",\"name\":\"").append(Util.json(m.name)).append("\"")
                    .append(",\"specialisation\":\"").append(Util.json(m.spec)).append("\"")
                    .append(",\"experience\":").append(m.years)
                    .append(",\"city\":\"").append(Util.json(m.city)).append("\"")
                    .append(",\"photo\":\"").append(Util.json(m.photo == null ? "" : m.photo)).append("\"")
                    .append(",\"fee\":").append(m.fee)
                    .append(",\"rating\":").append(String.format(Locale.ROOT, "%.1f", m.rating))
                    .append(",\"reviews\":").append(m.reviews)
                    .append(",\"bio\":\"").append(Util.json(bio)).append("\"}");
        }
        out.append("]}");
        return out.toString();
    }

    private static String labels(List<LegalCategories.Cat> cats, String separator) {
        StringBuilder b = new StringBuilder();
        for (LegalCategories.Cat c : cats) {
            b.append(b.length() > 0 ? separator : "").append(c.label);
        }
        return b.toString();
    }

    /* ---------- conversation kept in the session ---------- */

    @SuppressWarnings("unchecked")
    private static List<Turn> history(HttpSession s) {
        synchronized (s) {
            Object o = s.getAttribute(HISTORY);
            return o instanceof ArrayList ? new ArrayList<>((ArrayList<Turn>) o) : new ArrayList<Turn>();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> previousAreas(HttpSession s) {
        synchronized (s) {
            Object o = s.getAttribute(AREAS);
            return o instanceof ArrayList ? new ArrayList<>((ArrayList<String>) o) : new ArrayList<String>();
        }
    }

    /** Stores one question and Gemini's answer, keeping only the latest messages (always starting with a question). */
    private static void remember(HttpSession s, String question, String answer) {
        synchronized (s) {
            ArrayList<Turn> h = new ArrayList<>(history(s));
            h.add(new Turn("user", question));
            h.add(new Turn("model", answer));
            while (h.size() > MAX_HISTORY || (!h.isEmpty() && !"user".equals(h.get(0).role))) {
                h.remove(0);
            }
            s.setAttribute(HISTORY, h);
        }
    }

    private static void rememberAreas(HttpSession s, List<LegalCategories.Cat> cats) {
        ArrayList<String> names = new ArrayList<>();
        for (LegalCategories.Cat c : cats) {
            names.add(c.label);
        }
        synchronized (s) {
            s.setAttribute(AREAS, names);
        }
    }

    private static void clearConversation(HttpSession s) {
        synchronized (s) {
            s.removeAttribute(HISTORY);
            s.removeAttribute(AREAS);
        }
    }

    private static boolean sameAreas(List<LegalCategories.Cat> cats, List<String> names) {
        if (cats.size() != names.size()) {
            return false;
        }
        for (LegalCategories.Cat c : cats) {
            boolean found = false;
            for (String n : names) {
                if (c.label.equalsIgnoreCase(n)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /* ---------- advocate suggestions ---------- */

    /** Active, admin-verified advocates whose profile lists the detected practice areas, best match first. */
    private List<Match> findAdvocates(List<LegalCategories.Cat> cats) throws SQLException {
        List<Match> matched = new ArrayList<>();
        List<Match> general = new ArrayList<>();
        if (cats.isEmpty()) {
            return matched;
        }
        try (Connection c = DB.get();
                PreparedStatement s = c.prepareStatement(
                        "SELECT u.id,u.name,p.specialisation,p.experience_years,p.bio,p.city,p.fee,p.profile_photo,"
                        + "COALESCE(rv.avg_r,0) avg_r,COALESCE(rv.n,0) n "
                        + "FROM users u LEFT JOIN advocate_profiles p ON p.user_id=u.id "
                        + "LEFT JOIN (SELECT advocate_id,AVG(rating) avg_r,COUNT(*) n FROM reviews GROUP BY advocate_id) rv "
                        + "ON rv.advocate_id=u.id "
                        + "WHERE u.role='ADVOCATE' AND u.status='ACTIVE' AND p.verification_status='APPROVED'");
                ResultSet r = s.executeQuery()) {
            while (r.next()) {
                Match m = new Match();
                m.id = r.getInt("id");
                m.name = r.getString("name");
                m.spec = r.getString("specialisation") == null ? "General Practice" : r.getString("specialisation");
                m.years = r.getInt("experience_years");
                m.bio = r.getString("bio");
                m.city = r.getString("city") == null ? "" : r.getString("city");
                m.photo = r.getString("profile_photo");
                m.fee = r.getInt("fee");
                m.rating = r.getDouble("avg_r");
                m.reviews = r.getInt("n");
                String spec = m.spec.toLowerCase(Locale.ROOT);
                String bio = m.bio == null ? "" : m.bio.toLowerCase(Locale.ROOT);
                for (int i = 0; i < cats.size(); i++) {
                    LegalCategories.Cat cat = cats.get(i);
                    if (spec.contains(cat.label.toLowerCase(Locale.ROOT))) {
                        m.score += (i == 0 ? 10 : 6);
                    }
                    int hits = 0;
                    for (String kw : cat.keywords) {
                        if (hits < 3 && bio.contains(kw)) {
                            hits++;
                        }
                    }
                    m.score += hits;
                }
                if (m.score >= 6) {
                    matched.add(m);
                } else if (spec.contains("general")) {
                    m.general = true;
                    general.add(m);
                }
            }
        }
        Comparator<Match> byBest = (x, y) -> {
            if (x.score != y.score) {
                return y.score - x.score;
            }
            int rc = Double.compare(y.rating, x.rating);
            if (rc != 0) {
                return rc;
            }
            if (x.years != y.years) {
                return y.years - x.years;
            }
            return x.name.compareToIgnoreCase(y.name);
        };
        matched.sort(byBest);
        if (matched.isEmpty()) {
            general.sort(byBest);
            matched = general;
        }
        return matched.size() > 5 ? new ArrayList<>(matched.subList(0, 5)) : matched;
    }

    /* ---------- Gemini ---------- */

    /**
     * Calls Gemini with the earlier conversation plus the new question.
     * Never throws; returns the answer text or an error message.
     */
    private Gem gemini(String key, String question, List<LegalCategories.Cat> keywordAreas, List<Turn> history) {
        Gem g = new Gem();
        try {
            String model = Config.get("GEMINI_MODEL", "gemini.model", "gemini-2.5-flash");
            String base = Config.get("GEMINI_BASE_URL", "gemini.base.url",
                    "https://generativelanguage.googleapis.com/v1beta/models/");
            String body = requestBody(question, keywordAreas, history);
            HttpURLConnection con = (HttpURLConnection) URI.create(
                    base + URLEncoder.encode(model, StandardCharsets.UTF_8) + ":generateContent").toURL().openConnection();
            con.setConnectTimeout(8000);
            con.setReadTimeout(30000);
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("x-goog-api-key", key);
            try (OutputStream os = con.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            int code = con.getResponseCode();
            InputStream is = code < 400 ? con.getInputStream() : con.getErrorStream();
            String raw = is == null ? "" : new String(is.readAllBytes(), StandardCharsets.UTF_8);
            if (code >= 400) {
                g.error = httpError(code, raw);
                return g;
            }

            StringBuilder sb = new StringBuilder();
            Matcher m = Pattern.compile("\"text\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(raw);
            while (m.find()) {
                sb.append(unescape(m.group(1)));
            }
            String text = sb.toString().trim();

            // the first line is "AREA: ..." (the practice area Gemini identified); it is not shown to the client
            Matcher am = AREA_LINE.matcher(text);
            if (am.find()) {
                g.areas = parseAreas(am.group(1));
                text = text.substring(am.end());
            }
            text = limit(plain(text), MAX_ANSWER);
            if (text.isEmpty()) {
                boolean blocked = raw.contains("\"blockReason\"") || Pattern.compile("\"finishReason\"\\s*:\\s*\"SAFETY\"").matcher(raw).find();
                g.error = blocked
                        ? "Gemini declined to answer this question (safety filter). Please rephrase it."
                        : "Gemini returned an empty answer. Please try rephrasing your question.";
                return g;
            }
            g.text = text;
        } catch (SocketTimeoutException e) {
            g.error = "Gemini took too long to respond. Please try again.";
        } catch (Exception e) {
            g.error = "Could not reach Gemini (" + e.getClass().getSimpleName() + "). Check the server's internet connection.";
        }
        return g;
    }

    private static String httpError(int code, String raw) {
        Matcher em = Pattern.compile("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(raw);
        String detail = em.find() ? unescape(em.group(1)) : "";
        if (detail.length() > 140) {
            detail = detail.substring(0, 140) + "...";
        }
        String hint = "";
        if (code == 400 || code == 403) {
            hint = " (check gemini.api.key and gemini.model)";
        } else if (code == 404) {
            hint = " (model not found: check gemini.model)";
        } else if (code == 429) {
            hint = " (quota or rate limit reached: wait a moment and try again)";
        }
        return "Gemini returned HTTP " + code + (detail.isEmpty() ? "" : ": " + detail) + hint;
    }

    /** JSON for generateContent: instructions, the earlier conversation, then the new question. */
    private static String requestBody(String question, List<LegalCategories.Cat> keywordAreas, List<Turn> history) {
        StringBuilder b = new StringBuilder();
        b.append("{\"systemInstruction\":{\"parts\":[{\"text\":\"")
                .append(Util.json(systemPrompt(keywordAreas))).append("\"}]},\"contents\":[");
        for (Turn t : history) {
            b.append("{\"role\":\"").append(t.role).append("\",\"parts\":[{\"text\":\"")
                    .append(Util.json(t.text)).append("\"}]},");
        }
        b.append("{\"role\":\"user\",\"parts\":[{\"text\":\"").append(Util.json(question)).append("\"}]}],")
                .append("\"generationConfig\":{\"temperature\":0.4,\"maxOutputTokens\":4096}}");
        return b.toString();
    }

    private static String systemPrompt(List<LegalCategories.Cat> keywordAreas) {
        StringBuilder areas = new StringBuilder();
        for (LegalCategories.Cat c : LegalCategories.ALL) {
            if (!"General Practice".equals(c.label)) {
                areas.append(areas.length() > 0 ? ", " : "").append(c.label);
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("You are a careful legal information assistant on an Indian advocate booking portal called Advocate Connect. ")
                .append("Reply in simple plain English as plain text, with no markdown symbols such as asterisks, hashes or backticks. ")
                .append("When the user describes a new legal situation, write about 150 to 250 words covering: ")
                .append("(1) which area of law this is and the main Indian laws or concepts generally involved, ")
                .append("(2) the usual options or steps such as notice, mediation or filing a case, ")
                .append("(3) what to expect and common time limits in general terms, ")
                .append("(4) what to discuss with an advocate. ")
                .append("When the user asks a follow-up question, use the earlier conversation and answer that specific question ")
                .append("directly and concisely, without repeating the earlier overview. ")
                .append("Give general information only, never guarantee an outcome, never claim to be a lawyer, ")
                .append("and tell the user to consult a licensed advocate about their own case. ")
                .append("If the message is not about a legal matter, say briefly that you can only help with legal information ")
                .append("and invite the user to describe their legal problem. ")
                .append("Never ask for passwords, identity numbers or other highly sensitive details. ")
                .append("The portal's practice areas are: ").append(areas).append(". ");
        if (!keywordAreas.isEmpty()) {
            sb.append("Keyword matching on the latest message suggests: ").append(labels(keywordAreas, ", "))
                    .append(" (a hint only). ");
        }
        sb.append("Begin your reply with exactly one line in the form AREA: followed by one or two of those practice areas ")
                .append("separated by the | character, or AREA: NONE if no practice area fits, then a blank line, then your answer.");
        return sb.toString();
    }

    /** Turns "Property Law | Civil Law" into practice areas (at most two); unknown names and NONE are ignored. */
    private static List<LegalCategories.Cat> parseAreas(String line) {
        List<LegalCategories.Cat> out = new ArrayList<>();
        for (String token : line.split("[|,;]")) {
            String name = token.replaceAll("[*`_]", "").trim();
            if (name.length() < 4 || "none".equalsIgnoreCase(name)) {
                continue;
            }
            LegalCategories.Cat found = LegalCategories.byLabel(name);
            if (found == null) {
                String lower = name.toLowerCase(Locale.ROOT);
                for (LegalCategories.Cat c : LegalCategories.ALL) {
                    String label = c.label.toLowerCase(Locale.ROOT);
                    if (lower.contains(label) || label.contains(lower)) {
                        found = c;
                        break;
                    }
                }
            }
            if (found != null && !"General Practice".equals(found.label) && !out.contains(found) && out.size() < 2) {
                out.add(found);
            }
        }
        return out;
    }

    /** Removes the markdown Gemini sometimes adds anyway, so the chat bubble shows clean text. */
    private static String plain(String t) {
        t = t.replace("\r", "").replace("**", "").replace("`", "");
        t = t.replaceAll("(?m)^\\s*#{1,6}\\s*", "");
        t = t.replaceAll("(?m)^(\\s*)[*\\u2022]\\s+", "$1- ");
        t = t.replaceAll("\\n{3,}", "\n\n");
        return t.trim();
    }

    /** Shortens very long answers, preferably at the end of a sentence. */
    private static String limit(String t, int max) {
        if (t.length() <= max) {
            return t;
        }
        int cut = Math.max(t.lastIndexOf('.', max), t.lastIndexOf('\n', max));
        if (cut < max / 2) {
            return t.substring(0, max).trim() + "...";
        }
        return t.substring(0, cut + 1).trim();
    }

    private static String unescape(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '\\' || i + 1 >= s.length()) {
                b.append(ch);
                continue;
            }
            char n = s.charAt(++i);
            switch (n) {
                case 'n':
                    b.append('\n');
                    break;
                case 'r':
                    break;
                case 't':
                    b.append(' ');
                    break;
                case 'u':
                    if (i + 4 < s.length()) {
                        try {
                            b.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                            i += 4;
                        } catch (NumberFormatException e) {
                            b.append('u');
                        }
                    }
                    break;
                default:
                    b.append(n);
            }
        }
        return b.toString();
    }
}
