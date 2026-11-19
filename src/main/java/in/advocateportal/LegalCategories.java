package in.advocateportal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Practice areas used by advocate profiles, the directory filter and the AI legal guide.
 *
 * The AI guide matches keywords in the client's description to a practice area and then
 * suggests advocates whose profile lists that practice area. Every area also carries a few
 * "hints": ready-made keyword phrases the guide offers as one-tap suggestions so the client
 * can add more detail to the question.
 */
public final class LegalCategories {

    private LegalCategories() {
    }

    public static final class Cat {

        public final String label;
        public final String summary;
        public final String[] keywords;
        public final String[] hints;
        public final String[] documents;
        public final String[] questions;

        Cat(String label, String summary, String[] keywords, String[] hints, String[] documents, String[] questions) {
            this.label = label;
            this.summary = summary;
            this.keywords = keywords;
            this.hints = hints;
            this.documents = documents;
            this.questions = questions;
        }
    }

    private static String[] a(String... x) {
        return x;
    }

    /** Starter keywords offered when nothing in the question matched a practice area. */
    private static final String[] STARTERS = a(
            "property dispute", "divorce", "child custody", "cheque bounce", "job termination", "unpaid salary",
            "cyber fraud", "consumer refund", "police FIR", "legal notice", "motor accident", "income tax notice");

    public static final List<Cat> ALL = Collections.unmodifiableList(Arrays.asList(
            new Cat(
                    "Property Law",
                    "Land, house, flat, tenancy, ownership and inheritance-of-property matters.",
                    a("property", "land", "plot", "flat", "apartment", "house", "tenant", "landlord", "rent", "rental",
                            "lease", "eviction", "evict", "encroachment", "boundary", "sale deed", "registry", "mutation",
                            "title", "possession", "ancestral", "partition", "inheritance", "builder", "rera",
                            "illegal occupation", "property dispute", "tenancy", "security deposit", "illegal construction",
                            "demolition", "sale agreement", "land grabbing", "succession", "patta", "khata"),
                    a("boundary dispute", "tenant eviction", "ancestral property partition", "sale deed registry",
                            "illegal possession of land", "builder delay RERA", "rent agreement"),
                    a("Sale deed / title deed", "Property tax receipts and encumbrance certificate",
                            "Rent or lease agreement", "Any notices, letters or photos of the dispute"),
                    a("Is my title clear and what are my options?", "Should I send a legal notice first?",
                            "How long could this take and what are the likely costs?")),

            new Cat(
                    "Family Law",
                    "Marriage, divorce, custody, maintenance and domestic matters.",
                    a("divorce", "separation", "separated", "custody", "maintenance", "alimony", "marriage", "husband",
                            "wife", "dowry", "domestic violence", "child support", "adoption", "guardianship", "annulment",
                            "matrimonial", "in-laws", "mutual consent", "498a", "cruelty", "bigamy", "judicial separation",
                            "visitation rights", "streedhan", "domestic abuse"),
                    a("mutual consent divorce", "child custody", "maintenance and alimony", "dowry harassment 498a",
                            "domestic violence", "judicial separation"),
                    a("Marriage certificate", "ID and address proof of both parties", "Details of income and assets",
                            "Photos, messages or records relevant to the dispute"),
                    a("Mutual consent or contested divorce - which fits my case?", "What about child custody and maintenance?",
                            "What interim protection is available?")),

            new Cat(
                    "Criminal Law",
                    "FIRs, arrests, bail, police complaints and criminal defence.",
                    a("police", "fir", "arrest", "arrested", "bail", "theft", "assault", "murder", "threat", "threatened",
                            "harassment", "jail", "crime", "criminal", "cheating", "forgery", "complaint", "defamation",
                            "extortion", "false case", "anticipatory bail", "chargesheet", "police station", "summons",
                            "remand", "false allegation", "bns", "bnss", "ipc", "crpc"),
                    a("anticipatory bail", "false FIR", "arrest by police", "threat and harassment", "theft complaint",
                            "cheating and forgery"),
                    a("Copy of FIR or complaint", "Arrest memo or notices received", "ID proof",
                            "Any evidence: messages, photos, witnesses"),
                    a("Can I apply for bail or anticipatory bail?", "How do I respond to this police notice?",
                            "What are the next court dates?")),

            new Cat(
                    "Civil Law",
                    "Civil suits, money recovery, injunctions and compensation claims.",
                    a("civil", "suit", "injunction", "recovery", "damages", "compensation", "breach", "agreement dispute",
                            "money owed", "unpaid", "legal notice", "decree", "stay order", "civil case",
                            "specific performance", "civil court", "ex parte", "limitation period"),
                    a("money recovery suit", "injunction", "unpaid dues legal notice", "compensation for damages",
                            "breach of agreement dispute", "civil case", "stay order"),
                    a("Agreement or contract", "Payment proofs and bank statements", "Notices and replies exchanged",
                            "Timeline of events"),
                    a("Is a legal notice or a civil suit the better first step?",
                            "What is the limitation period for my claim?", "Can I get an injunction?")),

            new Cat(
                    "Corporate & Business Law",
                    "Company set-up, partnerships, contracts and business disputes.",
                    a("company", "startup", "business", "partnership", "llp", "shareholder", "incorporation", "merger",
                            "director", "founder", "vendor", "mou", "nda", "franchise", "commercial", "insolvency", "nclt",
                            "ibc", "shareholders", "co-founder", "cofounder", "joint venture", "arbitration"),
                    a("partnership dispute", "shareholder dispute", "startup contract", "NDA or MoU", "director removal",
                            "LLP setup"),
                    a("Company or partnership documents", "Contracts, MoUs or NDAs", "Shareholding details",
                            "Correspondence related to the dispute"),
                    a("Which business structure suits me?", "How do I protect myself in this contract?",
                            "How can this dispute be resolved - mediation or court?")),

            new Cat(
                    "Labour & Employment Law",
                    "Job termination, unpaid wages, workplace issues and employment contracts.",
                    a("employer", "employee", "salary", "termination", "terminated", "fired", "layoff", "wages", "pf",
                            "gratuity", "workplace", "resignation", "notice period", "job", "labour", "posh",
                            "sexual harassment at work", "unpaid salary", "wrongful termination", "retrenchment",
                            "provident fund", "relieving letter", "full and final settlement", "esi"),
                    a("unpaid salary", "wrongful termination", "gratuity and PF", "notice period dispute",
                            "forced resignation", "employer harassment at workplace"),
                    a("Appointment letter and employment contract", "Salary slips and bank statements",
                            "Termination or warning letters", "Emails or messages with the employer"),
                    a("Was my termination legal?", "Can I recover unpaid salary or gratuity?",
                            "What is the right forum for my complaint?")),

            new Cat(
                    "Consumer Protection",
                    "Defective products, poor service, refunds and unfair trade practices.",
                    a("consumer", "refund", "defective", "warranty", "e-commerce", "product", "deficiency", "overcharged",
                            "faulty", "misleading advertisement", "insurance claim", "insurance rejected", "delivery",
                            "consumer court", "consumer forum", "not refunded", "unfair trade practice",
                            "medical negligence"),
                    a("refund not received", "defective product", "warranty claim", "e-commerce delivery complaint",
                            "insurance claim rejected", "misleading advertisement"),
                    a("Bills, invoices and payment proof", "Warranty or policy documents", "Complaint emails and replies",
                            "Photos or videos of the defect"),
                    a("Should I approach the consumer commission?", "What compensation can I claim?",
                            "Is my complaint within the time limit?")),

            new Cat(
                    "Cyber Law",
                    "Online fraud, hacking, data leaks, impersonation and online harassment.",
                    a("cyber", "online fraud", "hacking", "hacked", "phishing", "social media", "data leak",
                            "identity theft", "morphed", "upi fraud", "blackmail", "scam", "sextortion", "fake profile",
                            "otp", "online harassment", "cybercrime", "cyber crime", "online scam", "credit card fraud",
                            "deepfake", "data breach", "ransomware", "loan app harassment"),
                    a("online fraud", "account hacked", "UPI fraud", "phishing scam", "fake profile", "data leak"),
                    a("Screenshots, URLs and chat records", "Transaction details and bank statement",
                            "Copy of complaint on cybercrime.gov.in", "Device or account details involved"),
                    a("What should I report first and where?", "Can the money be frozen or recovered?",
                            "How do I get the content removed?")),

            new Cat(
                    "Intellectual Property",
                    "Trademarks, copyright, patents, brand and design protection.",
                    a("trademark", "copyright", "patent", "plagiarism", "brand", "logo", "piracy", "infringement",
                            "intellectual property", "design registration", "trade mark", "trade secret", "passing off",
                            "cease and desist"),
                    a("trademark registration", "copyright infringement", "patent filing", "logo and brand protection",
                            "plagiarism", "piracy"),
                    a("Proof of creation or first use", "Registration certificates, if any",
                            "Examples of the infringement", "Business or brand details"),
                    a("Is my brand or work protected?", "How do I send a cease-and-desist notice?",
                            "What registration should I apply for?")),

            new Cat(
                    "Tax Law",
                    "Income tax, GST, tax notices and assessments.",
                    a("income tax", "gst", "tax notice", "itr", "tax evasion", "customs", "tax", "taxes", "assessment",
                            "tds", "tax refund", "tax demand", "reassessment", "tax audit", "input tax credit", "itc"),
                    a("income tax notice", "GST notice", "ITR filing", "tax refund delay", "tax assessment order",
                            "customs duty"),
                    a("Tax returns and assessment orders", "Notices received", "Books of accounts and invoices",
                            "PAN and GST details"),
                    a("How should I reply to this tax notice?", "What is the deadline to respond?",
                            "Can I appeal this order?")),

            new Cat(
                    "Motor Accident & Insurance",
                    "Road accidents, injury compensation and insurance claims.",
                    a("accident", "mact", "vehicle", "car crash", "bike accident", "injury", "injured", "hit and run",
                            "motor", "insurer", "road accident", "fatal accident", "drunk driving", "third party insurance",
                            "claims tribunal"),
                    a("road accident compensation", "hit and run", "vehicle accident", "bike accident", "MACT claim",
                            "injured in car crash"),
                    a("FIR and accident report", "Medical records and bills", "Vehicle and insurance papers",
                            "Driver and witness details"),
                    a("What compensation can I claim?", "How long does a motor accident claim take?",
                            "What if the vehicle was uninsured?")),

            new Cat(
                    "Banking & Cheque Bounce",
                    "Cheque dishonour, loan recovery, EMI and banking disputes.",
                    a("cheque", "bounce", "bounced", "dishonour", "section 138", "loan", "emi", "bank", "nbfc", "debt",
                            "mortgage", "credit card", "recovery agent", "sarfaesi", "debt recovery tribunal", "cibil",
                            "loan default", "guarantor"),
                    a("cheque bounce", "section 138 notice", "EMI default", "credit card debt", "bank loan settlement",
                            "loan recovery agent"),
                    a("Dishonoured cheque and bank return memo", "Loan agreement and statements",
                            "Legal notice and postal proof", "Payment history"),
                    a("Within what time must I send the legal notice?", "What happens if I cannot repay?",
                            "Can this be settled out of court?")),

            new Cat(
                    "General Practice",
                    "General legal advice across several areas.",
                    a(), a(), a(), a())
    ));

    public static Cat byLabel(String label) {
        if (label == null) {
            return null;
        }
        for (Cat c : ALL) {
            if (c.label.equalsIgnoreCase(label.trim())) {
                return c;
            }
        }
        return null;
    }

    /** Returns up to {@code max} practice areas that best match the text, best first. */
    public static List<Cat> detect(String text, int max) {
        List<Cat> out = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return out;
        }
        String t = text.toLowerCase(Locale.ROOT);
        final Map<Cat, Integer> scores = new LinkedHashMap<>();
        for (Cat c : ALL) {
            int score = 0;
            for (String kw : c.keywords) {
                // short keywords must match a whole word; longer ones may match as a word prefix (rent -> rental)
                boolean strict = kw.length() <= 4;
                String re = "(?<![a-z0-9])" + Pattern.quote(kw) + (strict ? "(?![a-z0-9])" : "");
                if (Pattern.compile(re).matcher(t).find()) {
                    score += kw.indexOf(' ') >= 0 ? 2 : 1;
                }
            }
            if (score > 0) {
                scores.put(c, score);
            }
        }
        List<Cat> sorted = new ArrayList<>(scores.keySet());
        sorted.sort((x, y) -> scores.get(y) - scores.get(x));
        for (Cat c : sorted) {
            if (out.size() < max) {
                out.add(c);
            }
        }
        return out;
    }

    /**
     * One-tap keyword suggestions for the AI legal guide.
     *
     * With no detected practice area it returns general starter keywords. Otherwise it returns
     * hints from the detected areas (taking turns between them) so the client can add more detail.
     * Phrases that already appear in the question are skipped.
     */
    public static List<String> suggestKeywords(String text, List<Cat> detected, int max) {
        String t = text == null ? "" : text.toLowerCase(Locale.ROOT);
        Set<String> out = new LinkedHashSet<>();
        if (detected == null || detected.isEmpty()) {
            for (String s : STARTERS) {
                if (out.size() < max && !t.contains(s.toLowerCase(Locale.ROOT))) {
                    out.add(s);
                }
            }
            return new ArrayList<>(out);
        }
        int longest = 0;
        for (Cat c : detected) {
            longest = Math.max(longest, c.hints.length);
        }
        for (int i = 0; i < longest && out.size() < max; i++) {
            for (Cat c : detected) {
                if (i < c.hints.length && out.size() < max && !t.contains(c.hints[i].toLowerCase(Locale.ROOT))) {
                    out.add(c.hints[i]);
                }
            }
        }
        return new ArrayList<>(out);
    }
}
