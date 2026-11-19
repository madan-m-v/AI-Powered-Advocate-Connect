package in.advocateportal;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.sql.*;

@WebServlet(urlPatterns = {"/login", "/register", "/logout"})
public class Auth extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest q, HttpServletResponse p) throws IOException {
        q.setCharacterEncoding("UTF-8");
        String path = q.getServletPath();
        try (Connection c = DB.get()) {
            if (path.equals("/register")) {
                register(c, q, p);
            } else {
                login(c, q, p);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            p.sendRedirect("register.jsp?error=Email+already+registered");
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void register(Connection c, HttpServletRequest q, HttpServletResponse p) throws Exception {
        String name = trim(q.getParameter("name"));
        String email = trim(q.getParameter("email")).toLowerCase();
        String phone = trim(q.getParameter("phone"));
        String role = q.getParameter("role");
        String pass = q.getParameter("password");
        boolean advocate = "ADVOCATE".equals(role);
        String barNumber = trim(q.getParameter("barNumber")).toUpperCase();
        String barState = trim(q.getParameter("barState"));
        int enrolYear = parseYear(q.getParameter("enrolmentYear"));
        boolean bad = name.isEmpty() || name.length() > 120 || email.length() > 190 || !email.contains("@")
                || phone.length() > 20 || pass == null || pass.length() < 8 || pass.length() > 100
                || !("CLIENT".equals(role) || "ADVOCATE".equals(role));
        if (bad) {
            p.sendRedirect("register.jsp?error=Please+check+your+details+and+use+an+8+character+password");
            return;
        }
        if (advocate) {
            if (!BarCouncils.validNumber(barNumber) || !BarCouncils.validState(barState)
                    || !BarCouncils.validYear(enrolYear)) {
                p.sendRedirect("register.jsp?error=Please+enter+a+valid+Bar+Council+enrolment+number,+state+and+year");
                return;
            }
            if (barNumberTaken(c, barNumber, barState, 0)) {
                p.sendRedirect("register.jsp?error=This+Bar+Council+enrolment+number+is+already+registered+for+that+state");
                return;
            }
        }
        int newId;
        try (PreparedStatement s = c.prepareStatement(
                "INSERT INTO users(name,email,phone,password,role) VALUES(?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1, name);
            s.setString(2, email);
            s.setString(3, phone);
            s.setString(4, pass);
            s.setString(5, role);
            s.executeUpdate();
            try (ResultSet k = s.getGeneratedKeys()) {
                k.next();
                newId = k.getInt(1);
            }
        }
        if (advocate) {
            // profile row with the KYC details; stays PENDING until an administrator approves it
            try (PreparedStatement s = c.prepareStatement(
                    "INSERT INTO advocate_profiles(user_id,bar_council_number,bar_council_state,enrolment_year,verification_status) "
                    + "VALUES(?,?,?,?,'PENDING')")) {
                s.setInt(1, newId);
                s.setString(2, barNumber);
                s.setString(3, barState);
                s.setInt(4, enrolYear);
                s.executeUpdate();
            }
        }
        p.sendRedirect("login.jsp?registered=" + (advocate ? "advocate" : "1"));
    }

    private void login(Connection c, HttpServletRequest q, HttpServletResponse p) throws Exception {
        String email = trim(q.getParameter("email")).toLowerCase();
        String pass = q.getParameter("password");
        if (!email.isEmpty() && pass != null) {
            try (PreparedStatement s = c.prepareStatement(
                    "SELECT id,name,password,role,status FROM users WHERE email=?")) {
                s.setString(1, email);
                try (ResultSet r = s.executeQuery()) {
                    if (r.next() && "ACTIVE".equals(r.getString("status"))
                            && pass.equals(r.getString("password"))) {
                        HttpSession old = q.getSession(false);
                        if (old != null) old.invalidate();
                        HttpSession session = q.getSession(true);
                        session.setAttribute("uid", r.getInt("id"));
                        session.setAttribute("name", r.getString("name"));
                        session.setAttribute("role", r.getString("role"));
                        p.sendRedirect("dashboard");
                        return;
                    }
                }
            }
        }
        p.sendRedirect("login.jsp?error=Invalid+credentials+or+account+disabled");
    }

    @Override
    protected void doGet(HttpServletRequest q, HttpServletResponse p) throws IOException {
        HttpSession s = q.getSession(false);
        if (s != null) s.invalidate();
        p.sendRedirect("index.jsp");
    }

    /** True when another advocate already uses this enrolment number in the same state council. */
    static boolean barNumberTaken(Connection c, String number, String state, int exceptUserId) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "SELECT 1 FROM advocate_profiles WHERE bar_council_number=? AND bar_council_state=? AND user_id<>?")) {
            s.setString(1, number);
            s.setString(2, state);
            s.setInt(3, exceptUserId);
            try (ResultSet r = s.executeQuery()) {
                return r.next();
            }
        }
    }

    private static int parseYear(String s) {
        try {
            return Integer.parseInt(trim(s));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
