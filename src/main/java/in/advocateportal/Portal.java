package in.advocateportal;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.nio.file.Files;
import java.sql.*;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@MultipartConfig(maxFileSize = 2 * 1024 * 1024, maxRequestSize = 3 * 1024 * 1024, fileSizeThreshold = 1024 * 1024)
@WebServlet(urlPatterns = {"/dashboard", "/book", "/cancel-appointment", "/reschedule", "/review-save",
    "/appointment-action", "/profile-save", "/verification-save", "/account-save", "/password-change",
    "/admin-action"})
public class Portal extends HttpServlet {

    private Integer uid(HttpServletRequest q) {
        HttpSession s = q.getSession(false);
        Object x = s == null ? null : s.getAttribute("uid");
        return x instanceof Integer ? (Integer) x : null;
    }

    private String role(HttpServletRequest q) {
        HttpSession s = q.getSession(false);
        return s == null ? null : (String) s.getAttribute("role");
    }

    @Override
    protected void doGet(HttpServletRequest q, HttpServletResponse p) throws ServletException, IOException {
        if (uid(q) == null) {
            p.sendRedirect("login.jsp");
            return;
        }
        String r = role(q), dest = "client.jsp";
        if ("ADMIN".equals(r)) {
            dest = "admin.jsp";
        } else if ("ADVOCATE".equals(r)) {
            dest = "advocate.jsp";
        }
        q.getRequestDispatcher("/" + dest).forward(q, p);
    }

    @Override
    protected void doPost(HttpServletRequest q, HttpServletResponse p) throws ServletException, IOException {
        q.setCharacterEncoding("UTF-8");
        Integer me = uid(q);
        if (me == null) {
            p.sendRedirect("login.jsp");
            return;
        }
        String path = q.getServletPath(), r = role(q);
        try (Connection c = DB.get()) {
            // any signed-in role
            if ("/account-save".equals(path)) {
                saveAccount(c, q, p, me);
                return;
            }
            if ("/password-change".equals(path)) {
                changePassword(c, q, p, me);
                return;
            }
            // role specific
            if ("CLIENT".equals(r)) {
                if ("/book".equals(path)) {
                    book(c, q, p, me);
                    return;
                }
                if ("/cancel-appointment".equals(path)) {
                    cancel(c, q, p, me);
                    return;
                }
                if ("/reschedule".equals(path)) {
                    reschedule(c, q, p, me);
                    return;
                }
                if ("/review-save".equals(path)) {
                    saveReview(c, q, p, me);
                    return;
                }
            }
            if ("ADVOCATE".equals(r)) {
                if ("/appointment-action".equals(path)) {
                    advocateAction(c, q, p, me);
                    return;
                }
                if ("/profile-save".equals(path)) {
                    saveProfile(c, q, p, me);
                    return;
                }
                if ("/verification-save".equals(path)) {
                    saveVerification(c, q, p, me);
                    return;
                }
            }
            if ("ADMIN".equals(r) && "/admin-action".equals(path)) {
                adminAction(c, q, p, me);
                return;
            }
            p.sendError(403);
        } catch (IllegalArgumentException | DateTimeException e) {
            String back = "dashboard?msg=invalid";
            if ("/profile-save".equals(path) || "/verification-save".equals(path)) {
                back = "profile.jsp?msg=invalid";
            } else if ("/account-save".equals(path) || "/password-change".equals(path)) {
                back = "account.jsp?msg=invalid";
            }
            p.sendRedirect(back);
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    /* ---------- shared slot checks ---------- */
    private boolean slotTaken(Connection c, int aid, LocalDate d, LocalTime t) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "SELECT 1 FROM appointments WHERE advocate_id=? AND appointment_date=? AND appointment_time=? AND status='APPROVED'")) {
            s.setInt(1, aid);
            s.setDate(2, Date.valueOf(d));
            s.setTime(3, Time.valueOf(t));
            try (ResultSet rs = s.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean duplicate(Connection c, int me, int aid, LocalDate d, LocalTime t, int exceptId) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "SELECT 1 FROM appointments WHERE client_id=? AND advocate_id=? AND appointment_date=? AND appointment_time=? "
                + "AND status='PENDING' AND id<>?")) {
            s.setInt(1, me);
            s.setInt(2, aid);
            s.setDate(3, Date.valueOf(d));
            s.setTime(4, Time.valueOf(t));
            s.setInt(5, exceptId);
            try (ResultSet rs = s.executeQuery()) {
                return rs.next();
            }
        }
    }

    /* ---------- client: book a consultation ---------- */
    private void book(Connection c, HttpServletRequest q, HttpServletResponse p, int me) throws SQLException, IOException {
        int aid = Integer.parseInt(q.getParameter("advocateId"));
        LocalDate date = LocalDate.parse(q.getParameter("appointmentDate"));
        LocalTime time = LocalTime.parse(q.getParameter("appointmentTime"));
        String topic = trim(q.getParameter("topic")), details = trim(q.getParameter("details"));
        if (topic.isEmpty() || topic.length() > 160 || details.length() > 2000) {
            p.sendRedirect("dashboard?msg=invalid");
            return;
        }
        if (LocalDateTime.of(date, time).isBefore(LocalDateTime.now())) {
            p.sendRedirect("dashboard?advocate=" + aid + "&msg=past#book");
            return;
        }
        try (PreparedStatement s = c.prepareStatement(
                "SELECT 1 FROM users u JOIN advocate_profiles p ON p.user_id=u.id "
                + "WHERE u.id=? AND u.role='ADVOCATE' AND u.status='ACTIVE' AND p.verification_status='APPROVED'")) {
            s.setInt(1, aid);
            try (ResultSet rs = s.executeQuery()) {
                if (!rs.next()) {
                    p.sendRedirect("dashboard?msg=invalid");
                    return;
                }
            }
        }
        if (slotTaken(c, aid, date, time)) {
            p.sendRedirect("dashboard?advocate=" + aid + "&msg=slot#book");
            return;
        }
        if (duplicate(c, me, aid, date, time, 0)) {
            p.sendRedirect("dashboard?advocate=" + aid + "&msg=dup#book");
            return;
        }
        try (PreparedStatement s = c.prepareStatement(
                "INSERT INTO appointments(client_id,advocate_id,appointment_date,appointment_time,topic,details,status) "
                + "VALUES(?,?,?,?,?,?,'PENDING')")) {
            s.setInt(1, me);
            s.setInt(2, aid);
            s.setDate(3, Date.valueOf(date));
            s.setTime(4, Time.valueOf(time));
            s.setString(5, topic);
            s.setString(6, details);
            s.executeUpdate();
        }
        p.sendRedirect("dashboard?msg=booked");
    }

    /* ---------- client: cancel own request ---------- */
    private void cancel(Connection c, HttpServletRequest q, HttpServletResponse p, int me) throws SQLException, IOException {
        int id = Integer.parseInt(q.getParameter("id"));
        try (PreparedStatement s = c.prepareStatement(
                "UPDATE appointments SET status='CANCELLED' WHERE id=? AND client_id=? AND status IN ('PENDING','APPROVED')")) {
            s.setInt(1, id);
            s.setInt(2, me);
            s.executeUpdate();
        }
        p.sendRedirect("dashboard?msg=cancelled");
    }

    /* ---------- client: move a pending request to a new date/time ---------- */
    private void reschedule(Connection c, HttpServletRequest q, HttpServletResponse p, int me) throws SQLException, IOException {
        int id = Integer.parseInt(q.getParameter("id"));
        LocalDate date = LocalDate.parse(q.getParameter("appointmentDate"));
        LocalTime time = LocalTime.parse(q.getParameter("appointmentTime"));
        if (LocalDateTime.of(date, time).isBefore(LocalDateTime.now())) {
            p.sendRedirect("dashboard?msg=past");
            return;
        }
        int aid = -1;
        try (PreparedStatement s = c.prepareStatement("SELECT advocate_id FROM appointments WHERE id=? AND client_id=? AND status='PENDING'")) {
            s.setInt(1, id);
            s.setInt(2, me);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    aid = rs.getInt(1);
                }
            }
        }
        if (aid < 0) {
            p.sendRedirect("dashboard?msg=invalid");
            return;
        }
        if (slotTaken(c, aid, date, time)) {
            p.sendRedirect("dashboard?msg=slot");
            return;
        }
        if (duplicate(c, me, aid, date, time, id)) {
            p.sendRedirect("dashboard?msg=dup");
            return;
        }
        try (PreparedStatement s = c.prepareStatement(
                "UPDATE appointments SET appointment_date=?,appointment_time=? WHERE id=? AND client_id=? AND status='PENDING'")) {
            s.setDate(1, Date.valueOf(date));
            s.setTime(2, Time.valueOf(time));
            s.setInt(3, id);
            s.setInt(4, me);
            s.executeUpdate();
        }
        p.sendRedirect("dashboard?msg=rescheduled");
    }

    /* ---------- client: rate a completed consultation ---------- */
    private void saveReview(Connection c, HttpServletRequest q, HttpServletResponse p, int me) throws SQLException, IOException {
        int id = Integer.parseInt(q.getParameter("id"));
        int rating = Integer.parseInt(q.getParameter("rating"));
        String comment = trim(q.getParameter("comment"));
        if (rating < 1 || rating > 5 || comment.length() > 500) {
            p.sendRedirect("dashboard?msg=invalid");
            return;
        }
        int aid = -1;
        try (PreparedStatement s = c.prepareStatement("SELECT advocate_id FROM appointments WHERE id=? AND client_id=? AND status='COMPLETED'")) {
            s.setInt(1, id);
            s.setInt(2, me);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    aid = rs.getInt(1);
                }
            }
        }
        if (aid < 0) {
            p.sendRedirect("dashboard?msg=invalid");
            return;
        }
        try (PreparedStatement s = c.prepareStatement(
                "INSERT INTO reviews(appointment_id,client_id,advocate_id,rating,comment) VALUES(?,?,?,?,?) "
                + "ON DUPLICATE KEY UPDATE rating=VALUES(rating),comment=VALUES(comment)")) {
            s.setInt(1, id);
            s.setInt(2, me);
            s.setInt(3, aid);
            s.setInt(4, rating);
            s.setString(5, comment);
            s.executeUpdate();
        }
        p.sendRedirect("dashboard?msg=reviewed");
    }

    /* ---------- advocate: approve / reject / complete / cancel (with optional note) ---------- */
    private void advocateAction(Connection c, HttpServletRequest q, HttpServletResponse p, int me) throws SQLException, IOException {
        String action = q.getParameter("action");
        int id = Integer.parseInt(q.getParameter("id"));
        String note = trim(q.getParameter("note"));
        if (note.length() > 500) {
            note = note.substring(0, 500);
        }
        String allowedFrom;
        if ("APPROVED".equals(action) || "REJECTED".equals(action)) {
            allowedFrom = "('PENDING')";
        } else if ("COMPLETED".equals(action)) {
            allowedFrom = "('APPROVED')";
        } else if ("CANCELLED".equals(action)) {
            allowedFrom = "('PENDING','APPROVED')";
        } else {
            p.sendRedirect("dashboard?msg=invalid");
            return;
        }

        if ("APPROVED".equals(action)) {
            try (PreparedStatement s = c.prepareStatement(
                    "SELECT 1 FROM appointments a JOIN appointments b ON b.advocate_id=a.advocate_id "
                    + "AND b.appointment_date=a.appointment_date AND b.appointment_time=a.appointment_time "
                    + "AND b.status='APPROVED' AND b.id<>a.id WHERE a.id=? AND a.advocate_id=?")) {
                s.setInt(1, id);
                s.setInt(2, me);
                try (ResultSet rs = s.executeQuery()) {
                    if (rs.next()) {
                        p.sendRedirect("dashboard?msg=conflict");
                        return;
                    }
                }
            }
        }
        try (PreparedStatement s = c.prepareStatement(
                "UPDATE appointments SET status=?, advocate_note=IF(?='',advocate_note,?) WHERE id=? AND advocate_id=? AND status IN " + allowedFrom)) {
            s.setString(1, action);
            s.setString(2, note);
            s.setString(3, note);
            s.setInt(4, id);
            s.setInt(5, me);
            s.executeUpdate();
        }
        p.sendRedirect("dashboard?msg=updated");
    }

    /* ---------- advocate: save profile ---------- */
    private void saveProfile(Connection c, HttpServletRequest q, HttpServletResponse p, int me) throws SQLException, ServletException, IOException {
        List<String> chosen = new ArrayList<>();
        String[] sel = q.getParameterValues("spec");
        if (sel != null) {
            for (String s : sel) {
                LegalCategories.Cat cat = LegalCategories.byLabel(s);
                if (cat != null && !chosen.contains(cat.label) && chosen.size() < 4) {
                    chosen.add(cat.label);
                }
            }
        }
        if (chosen.isEmpty()) {
            p.sendRedirect("profile.jsp?msg=nospec");
            return;
        }
        int years = Math.max(0, Math.min(60, intOr0(q.getParameter("experience"))));
        int fee = Math.max(0, Math.min(1000000, intOr0(q.getParameter("fee"))));
        String city = cut(trim(q.getParameter("city")), 80);
        String languages = cut(trim(q.getParameter("languages")), 120);
        String bio = cut(trim(q.getParameter("bio")), 1000);
        String photoPath = saveAdvocatePhoto(q, me);
        String sql = photoPath == null
                ? "INSERT INTO advocate_profiles(user_id,specialisation,experience_years,city,languages,fee,bio) VALUES(?,?,?,?,?,?,?) "
                    + "ON DUPLICATE KEY UPDATE specialisation=VALUES(specialisation),experience_years=VALUES(experience_years),city=VALUES(city),languages=VALUES(languages),fee=VALUES(fee),bio=VALUES(bio)"
                : "INSERT INTO advocate_profiles(user_id,specialisation,experience_years,city,languages,fee,bio,profile_photo) VALUES(?,?,?,?,?,?,?,?) "
                    + "ON DUPLICATE KEY UPDATE specialisation=VALUES(specialisation),experience_years=VALUES(experience_years),city=VALUES(city),languages=VALUES(languages),fee=VALUES(fee),bio=VALUES(bio),profile_photo=VALUES(profile_photo)";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, me);
            s.setString(2, String.join(", ", chosen));
            s.setInt(3, years);
            s.setString(4, city);
            s.setString(5, languages);
            s.setInt(6, fee);
            s.setString(7, bio);
            if (photoPath != null) s.setString(8, photoPath);
            s.executeUpdate();
        }
        p.sendRedirect("profile.jsp?msg=saved");
    }

    /** Stores a validated advocate image under the application web root. */
    private String saveAdvocatePhoto(HttpServletRequest request, int userId) throws IOException, ServletException {
        Part part = request.getPart("profilePhoto");
        if (part == null || part.getSize() == 0) return null;
        if (part.getSize() > 2L * 1024 * 1024) throw new IllegalArgumentException("Image exceeds 2 MB");
        String contentType = part.getContentType();
        String extension;
        if ("image/jpeg".equalsIgnoreCase(contentType)) extension = ".jpg";
        else if ("image/png".equalsIgnoreCase(contentType)) extension = ".png";
        else if ("image/webp".equalsIgnoreCase(contentType)) extension = ".webp";
        else throw new IllegalArgumentException("Only JPG, PNG and WEBP images are supported");
        byte[] header = new byte[12];
        try (InputStream in = part.getInputStream()) {
            int count = in.read(header);
            boolean jpeg = count >= 3 && (header[0] & 255) == 0xFF && (header[1] & 255) == 0xD8 && (header[2] & 255) == 0xFF;
            boolean png = count >= 8 && (header[0] & 255) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47;
            boolean webp = count >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
            boolean valid = (".jpg".equals(extension) && jpeg) || (".png".equals(extension) && png)
                    || (".webp".equals(extension) && webp);
            if (!valid) throw new IllegalArgumentException("Image content does not match its file type");
        }
        String relative = "uploads/advocates/advocate-" + userId + "-" + UUID.randomUUID() + extension;
        String root = getServletContext().getRealPath("/");
        if (root == null) throw new IOException("Application upload directory is unavailable");
        File target = new File(root, relative);
        File parent = target.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) throw new IOException("Could not create upload directory");
        try (InputStream upload = part.getInputStream()) {
            Files.copy(upload, target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return relative;
    }

    /* ---------- advocate: submit or correct Bar Council details (goes back to PENDING for admin review) ---------- */
    private void saveVerification(Connection c, HttpServletRequest q, HttpServletResponse p, int me) throws SQLException, IOException {
        String number = trim(q.getParameter("barNumber")).toUpperCase();
        String state = trim(q.getParameter("barState"));
        int year;
        try {
            year = Integer.parseInt(trim(q.getParameter("enrolmentYear")));
        } catch (NumberFormatException e) {
            year = 0;
        }
        if (!BarCouncils.validNumber(number) || !BarCouncils.validState(state) || !BarCouncils.validYear(year)) {
            p.sendRedirect("profile.jsp?msg=vinvalid");
            return;
        }
        String current = null;
        try (PreparedStatement s = c.prepareStatement("SELECT verification_status FROM advocate_profiles WHERE user_id=?")) {
            s.setInt(1, me);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    current = rs.getString(1);
                }
            }
        }
        if ("APPROVED".equals(current)) {
            p.sendRedirect("profile.jsp?msg=vlocked");
            return;
        }
        if (Auth.barNumberTaken(c, number, state, me)) {
            p.sendRedirect("profile.jsp?msg=vdup");
            return;
        }
        try (PreparedStatement s = c.prepareStatement(
                "INSERT INTO advocate_profiles(user_id,bar_council_number,bar_council_state,enrolment_year,verification_status) "
                + "VALUES(?,?,?,?,'PENDING') "
                + "ON DUPLICATE KEY UPDATE bar_council_number=VALUES(bar_council_number),bar_council_state=VALUES(bar_council_state),"
                + "enrolment_year=VALUES(enrolment_year),verification_status='PENDING',verification_note=NULL,"
                + "verified_at=NULL,verified_by=NULL")) {
            s.setInt(1, me);
            s.setString(2, number);
            s.setString(3, state);
            s.setInt(4, year);
            s.executeUpdate();
        }
        p.sendRedirect("profile.jsp?msg=vsubmitted");
    }

    /* ---------- any user: name / phone ---------- */
    private void saveAccount(Connection c, HttpServletRequest q, HttpServletResponse p, int me) throws SQLException, IOException {
        String name = trim(q.getParameter("name")), phone = trim(q.getParameter("phone"));
        if (name.isEmpty() || name.length() > 120 || phone.length() > 20) {
            p.sendRedirect("account.jsp?msg=invalid");
            return;
        }
        try (PreparedStatement s = c.prepareStatement("UPDATE users SET name=?,phone=? WHERE id=?")) {
            s.setString(1, name);
            s.setString(2, phone);
            s.setInt(3, me);
            s.executeUpdate();
        }
        q.getSession().setAttribute("name", name);
        p.sendRedirect("account.jsp?msg=account");
    }

    /* ---------- any user: change password ---------- */
    private void changePassword(Connection c, HttpServletRequest q, HttpServletResponse p, int me) throws SQLException, IOException {
        String cur = q.getParameter("current"), nw = q.getParameter("newPassword"), again = q.getParameter("confirm");
        if (cur == null || nw == null || again == null) {
            p.sendRedirect("account.jsp?msg=invalid");
            return;
        }
        String stored = null;
        try (PreparedStatement s = c.prepareStatement("SELECT password FROM users WHERE id=?")) {
            s.setInt(1, me);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    stored = rs.getString(1);
                }
            }
        }
        if (stored == null || !stored.equals(cur)) {
            p.sendRedirect("account.jsp?msg=pwwrong");
            return;
        }
        if (nw.length() < 8 || nw.length() > 100) {
            p.sendRedirect("account.jsp?msg=pwshort");
            return;
        }
        if (!nw.equals(again)) {
            p.sendRedirect("account.jsp?msg=pwmismatch");
            return;
        }
        try (PreparedStatement s = c.prepareStatement("UPDATE users SET password=? WHERE id=?")) {
            s.setString(1, nw);
            s.setInt(2, me);
            s.executeUpdate();
        }
        p.sendRedirect("account.jsp?msg=pwchanged");
    }

    /* ---------- admin: enable / disable accounts, approve / reject advocate verification ---------- */
    private void adminAction(Connection c, HttpServletRequest q, HttpServletResponse p, int adminId) throws SQLException, IOException {
        String action = q.getParameter("action");
        int id = Integer.parseInt(q.getParameter("id"));
        if ("disable".equals(action) || "enable".equals(action)) {
            try (PreparedStatement s = c.prepareStatement("UPDATE users SET status=? WHERE id=? AND role<>'ADMIN'")) {
                s.setString(1, "disable".equals(action) ? "DISABLED" : "ACTIVE");
                s.setInt(2, id);
                s.executeUpdate();
            }
            p.sendRedirect("dashboard");
            return;
        }
        if ("approve-advocate".equals(action) || "reject-advocate".equals(action)) {
            boolean approve = "approve-advocate".equals(action);
            String note = cut(trim(q.getParameter("note")), 300);
            try (PreparedStatement s = c.prepareStatement(
                    "UPDATE advocate_profiles SET verification_status=?,verification_note=?,verified_at=NOW(),verified_by=? "
                    + "WHERE user_id=? AND user_id IN (SELECT id FROM users WHERE role='ADVOCATE')")) {
                s.setString(1, approve ? "APPROVED" : "REJECTED");
                s.setString(2, note.isEmpty() ? null : note);
                s.setInt(3, adminId);
                s.setInt(4, id);
                s.executeUpdate();
            }
            p.sendRedirect("dashboard?msg=" + (approve ? "vapproved" : "vrejected") + "#verification");
            return;
        }
        p.sendRedirect("dashboard?msg=invalid");
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String cut(String s, int n) {
        return s.length() > n ? s.substring(0, n) : s;
    }

    private static int intOr0(String s) {
        s = trim(s);
        return s.isEmpty() ? 0 : Integer.parseInt(s);
    }
}
