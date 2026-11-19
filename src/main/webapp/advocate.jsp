<%@ page contentType="text/html;charset=UTF-8" import="java.sql.*,java.time.*,in.advocateportal.*" %>
<%
    if (!"ADVOCATE".equals(session.getAttribute("role"))) {
        response.sendRedirect("login.jsp");
        return;
    }
    int myId = (Integer) session.getAttribute("uid");
    int total = 0, pending = 0, approved = 0, completed = 0, reviewCount = 0;
    double avgRating = 0;
    String mySpec = null, vStatus = "PENDING", vNote = "";
    try (Connection c = DB.get()) {
        try (PreparedStatement s = c.prepareStatement("SELECT status,COUNT(*) n FROM appointments WHERE advocate_id=? GROUP BY status")) {
            s.setInt(1, myId);
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    int n = r.getInt("n");
                    total += n;
                    String st = r.getString("status");
                    if ("PENDING".equals(st)) {
                        pending = n;
                    } else if ("APPROVED".equals(st)) {
                        approved = n;
                    } else if ("COMPLETED".equals(st)) {
                        completed = n;
                    }
                }
            }
        }
        try (PreparedStatement s = c.prepareStatement("SELECT specialisation,verification_status,verification_note FROM advocate_profiles WHERE user_id=?")) {
            s.setInt(1, myId);
            try (ResultSet r = s.executeQuery()) {
                if (r.next()) {
                    mySpec = r.getString(1);
                    vStatus = r.getString(2) == null ? "PENDING" : r.getString(2);
                    vNote = r.getString(3) == null ? "" : r.getString(3);
                }
            }
        }
        try (PreparedStatement s = c.prepareStatement("SELECT COALESCE(AVG(rating),0),COUNT(*) FROM reviews WHERE advocate_id=?")) {
            s.setInt(1, myId);
            try (ResultSet r = s.executeQuery()) {
                if (r.next()) {
                    avgRating = r.getDouble(1);
                    reviewCount = r.getInt(2);
                }
            }
        }
    } catch (Exception e) { }
    boolean profileIncomplete = mySpec == null || mySpec.trim().isEmpty() || mySpec.toLowerCase().contains("general");
%>
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Advocate dashboard | Advocate Connect</title>
    <link rel="stylesheet" href="assets/style.css">
</head>
<body>
    <%@ include file="includes/header.jspf" %>
    <main class="page">
        <div class="dash-head">
            <div>
                <span class="eyebrow">ADVOCATE WORKSPACE</span>
                <h1>Welcome, <%=Util.esc(session.getAttribute("name"))%></h1>
                <p class="lead">Review consultation requests and manage your schedule.</p>
            </div>
            <a class="btn primary" href="profile.jsp">Edit my profile</a>
        </div>
        <%=Util.flashHtml(request.getParameter("msg"))%>
        <% if ("PENDING".equals(vStatus)) { %>
            <div class="notice"><b>Verification pending.</b> An administrator is reviewing your Bar Council details. Clients will see you in the directory and can book you only after approval.</div>
        <% } else if ("REJECTED".equals(vStatus)) { %>
            <div class="alert"><b>Verification rejected.</b> <%=vNote.isEmpty() ? "" : Util.esc(vNote) + " "%>Please <a href="profile.jsp#verification"><b>correct your Bar Council details</b></a> and submit them again.</div>
        <% } %>
        <% if (profileIncomplete) { %>
            <div class="notice">Choose your practice areas on your <a href="profile.jsp"><b>profile</b></a> so the AI legal guide and the directory can suggest you to clients with matching cases (for example property or divorce).</div>
        <% } %>
        <div class="stats">
            <div><small>Pending requests</small><strong><%=pending%></strong></div>
            <div><small>Approved</small><strong><%=approved%></strong></div>
            <div><small>Completed</small><strong><%=completed%></strong></div>
            <div><small>All requests</small><strong><%=total%></strong></div>
            <div>
                <small>Rating</small>
                <strong><%=reviewCount > 0 ? "★ " + String.format(java.util.Locale.ROOT, "%.1f", avgRating) : "—"%></strong>
                <small><%=reviewCount%> review<%=reviewCount == 1 ? "" : "s"%></small>
            </div>
        </div>

        <section class="panel">
            <h2>Upcoming schedule</h2>
            <div class="table-wrap">
                <table>
                    <tr>
                        <th>Date</th>
                        <th>Time</th>
                        <th>Client</th>
                        <th>Topic</th>
                    </tr>
                    <%
                        boolean anyUp = false;
                        try (Connection c = DB.get(); PreparedStatement s = c.prepareStatement(
                                "SELECT a.appointment_date,a.appointment_time,a.topic,u.name FROM appointments a JOIN users u ON u.id=a.client_id "
                                + "WHERE a.advocate_id=? AND a.status='APPROVED' AND (a.appointment_date>CURDATE() OR (a.appointment_date=CURDATE() AND a.appointment_time>=CURTIME())) "
                                + "ORDER BY a.appointment_date,a.appointment_time LIMIT 8")) {
                            s.setInt(1, myId);
                            try (ResultSet r = s.executeQuery()) {
                                while (r.next()) {
                                    anyUp = true;
                                    boolean today = r.getDate(1).toLocalDate().equals(LocalDate.now());
                    %>
                    <tr>
                        <td><%=r.getDate(1)%><%=today ? " <span class=\"status APPROVED\">Today</span>" : ""%></td>
                        <td><%=r.getString(2).substring(0,5)%></td>
                        <td><%=Util.esc(r.getString(4))%></td>
                        <td><%=Util.esc(r.getString(3))%></td>
                    </tr>
                    <%
                                }
                            }
                        } catch (Exception e) { }
                        if (!anyUp) {
                    %>
                    <tr><td colspan="4" class="muted">No approved consultations coming up.</td></tr>
                    <%
                        }
                    %>
                </table>
            </div>
        </section>

        <section class="panel">
            <h2>Consultation requests</h2>
            <div class="dashboard-search">
                <input type="search" data-table-search="reqTable" placeholder="Search requests by client, date or legal topic…" aria-label="Search consultation requests">
            </div>
            <div class="tabs" data-tabs="reqTable">
                <button type="button" class="tab active" data-filter="ALL">All</button>
                <button type="button" class="tab" data-filter="PENDING">Pending</button>
                <button type="button" class="tab" data-filter="APPROVED">Approved</button>
                <button type="button" class="tab" data-filter="COMPLETED">Completed</button>
                <button type="button" class="tab" data-filter="CLOSED">Cancelled / rejected</button>
            </div>
            <div class="table-wrap">
                <table id="reqTable">
                    <tr>
                        <th>Client</th>
                        <th>Date &amp; time</th>
                        <th>Topic &amp; details</th>
                        <th>Status</th>
                        <th>Action</th>
                    </tr>
                    <%
                        boolean any = false;
                        try (Connection c = DB.get(); PreparedStatement s = c.prepareStatement(
                                "SELECT a.id,a.appointment_date,a.appointment_time,a.topic,a.details,a.status,a.advocate_note,u.name,u.email,u.phone "
                                + "FROM appointments a JOIN users u ON u.id=a.client_id WHERE a.advocate_id=? "
                                + "ORDER BY FIELD(a.status,'PENDING','APPROVED','COMPLETED','REJECTED','CANCELLED'),a.appointment_date,a.appointment_time")) {
                            s.setInt(1, myId);
                            try (ResultSet r = s.executeQuery()) {
                                while (r.next()) {
                                    any = true;
                                    String st = r.getString("status");
                                    String group = ("REJECTED".equals(st) || "CANCELLED".equals(st)) ? "CLOSED" : st;
                                    boolean showContact = "APPROVED".equals(st) || "COMPLETED".equals(st);
                                    String note = r.getString("advocate_note");
                    %>
                    <tr data-group="<%=group%>">
                        <td>
                            <b><%=Util.esc(r.getString("name"))%></b>
                            <% if (showContact) { %>
                                <br><small class="muted"><%=Util.esc(r.getString("email"))%><% if (r.getString("phone") != null && !r.getString("phone").isEmpty()) { %> · <%=Util.esc(r.getString("phone"))%><% } %></small>
                            <% } %>
                        </td>
                        <td>
                            <%=r.getDate("appointment_date")%><br>
                            <small class="muted"><%=r.getString("appointment_time").substring(0,5)%></small>
                        </td>
                        <td>
                            <b><%=Util.esc(r.getString("topic"))%></b><br>
                            <small class="muted"><%=Util.esc(r.getString("details"))%></small>
                            <% if (note != null && !note.isEmpty()) { %>
                                <div class="note">💬 Your note: <%=Util.esc(note)%></div>
                            <% } %>
                        </td>
                        <td><span class="status <%=st%>"><%=st%></span></td>
                        <td class="acts">
                            <% if ("PENDING".equals(st)) { %>
                                <form method="post" action="appointment-action" class="stack">
                                    <input type="hidden" name="id" value="<%=r.getInt("id")%>">
                                    <input name="note" maxlength="500" placeholder="Note to client (optional)">
                                    <div class="inline">
                                        <button name="action" value="APPROVED" class="tiny">Approve</button>
                                        <button name="action" value="REJECTED" class="tiny danger">Reject</button>
                                    </div>
                                </form>
                            <% } else if ("APPROVED".equals(st)) { %>
                                <form method="post" action="appointment-action" class="stack">
                                    <input type="hidden" name="id" value="<%=r.getInt("id")%>">
                                    <input name="note" maxlength="500" placeholder="Note to client (optional)">
                                    <div class="inline">
                                        <button name="action" value="COMPLETED" class="tiny">Mark completed</button>
                                        <button name="action" value="CANCELLED" class="tiny danger" onclick="return confirm('Cancel this approved appointment?');">Cancel</button>
                                    </div>
                                </form>
                            <% } %>
                        </td>
                    </tr>
                    <%
                                }
                            }
                        } catch (Exception e) {
                    %>
                    <tr><td colspan="5">Could not load requests right now.</td></tr>
                    <%
                        }
                        if (!any) {
                    %>
                    <tr><td colspan="5" class="muted">No consultation requests yet. Complete your profile so clients can find you.</td></tr>
                    <%
                        }
                    %>
                </table>
            </div>
        </section>

        <section class="panel">
            <h2>Recent client reviews</h2>
            <%
                boolean anyRev = false;
                try (Connection c = DB.get(); PreparedStatement s = c.prepareStatement(
                        "SELECT rv.rating,rv.comment,rv.created_at,u.name FROM reviews rv JOIN users u ON u.id=rv.client_id WHERE rv.advocate_id=? ORDER BY rv.created_at DESC LIMIT 5")) {
                    s.setInt(1, myId);
                    try (ResultSet r = s.executeQuery()) {
                        while (r.next()) {
                            anyRev = true;
            %>
            <div class="review">
                <b><%=Util.esc(r.getString("name"))%></b> <span class="stars"><%="★★★★★".substring(0, r.getInt("rating"))%></span><br>
                <small class="muted"><%=Util.esc(r.getString("comment"))%></small>
            </div>
            <%
                        }
                    }
                } catch (Exception e) { }
                if (!anyRev) {
            %>
            <p class="muted">Reviews appear here after clients rate completed consultations.</p>
            <%
                }
            %>
        </section>
    </main>
    <%@ include file="includes/foot.jspf" %>
</body>
</html>
