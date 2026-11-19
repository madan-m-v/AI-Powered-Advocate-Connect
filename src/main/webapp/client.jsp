<%@ page contentType="text/html;charset=UTF-8" import="java.sql.*,java.time.*,in.advocateportal.*" %>
<%
    if (!"CLIENT".equals(session.getAttribute("role"))) {
        response.sendRedirect("login.jsp");
        return;
    }
    int myId = (Integer) session.getAttribute("uid");
    int total = 0, pending = 0, approved = 0, completed = 0;
    String nextText = null;
    try (Connection c = DB.get()) {
        try (PreparedStatement s = c.prepareStatement("SELECT status,COUNT(*) n FROM appointments WHERE client_id=? GROUP BY status")) {
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
        try (PreparedStatement s = c.prepareStatement(
                "SELECT a.appointment_date,a.appointment_time,u.name FROM appointments a JOIN users u ON u.id=a.advocate_id "
                + "WHERE a.client_id=? AND a.status='APPROVED' AND (a.appointment_date>CURDATE() OR (a.appointment_date=CURDATE() AND a.appointment_time>=CURTIME())) "
                + "ORDER BY a.appointment_date,a.appointment_time LIMIT 1")) {
            s.setInt(1, myId);
            try (ResultSet r = s.executeQuery()) {
                if (r.next()) {
                    nextText = r.getDate(1) + " at " + r.getString(2).substring(0, 5) + " with " + r.getString(3);
                }
            }
        }
    } catch (Exception e) { }
    String pre = request.getParameter("advocate");
    String preTopic = request.getParameter("topic") == null ? "" : request.getParameter("topic");
%>
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Client dashboard | Advocate Connect</title>
    <link rel="stylesheet" href="assets/style.css">
</head>
<body>
    <%@ include file="includes/header.jspf" %>
    <main class="page">
        <div class="dash-head">
            <div>
                <span class="eyebrow">CLIENT WORKSPACE</span>
                <h1>Hello, <%=Util.esc(session.getAttribute("name"))%></h1>
                <p class="lead">Track your consultation requests and book your next step.</p>
            </div>
            <div class="actions">
                <a class="btn ghost" href="ai.jsp">Ask AI legal guide</a>
                <a class="btn primary" href="advocates.jsp">+ Find an advocate</a>
            </div>
        </div>
        <%=Util.flashHtml(request.getParameter("msg"))%>
        <% if (nextText != null) { %>
            <div class="banner">📅 <b>Next appointment:</b> <%=Util.esc(nextText)%></div>
        <% } %>
        <div class="stats">
            <div><small>Total requests</small><strong><%=total%></strong></div>
            <div><small>Waiting for reply</small><strong><%=pending%></strong></div>
            <div><small>Approved</small><strong><%=approved%></strong></div>
            <div><small>Completed</small><strong><%=completed%></strong></div>
        </div>

        <section class="panel">
            <h2>My appointments</h2>
            <div class="dashboard-search">
                <input type="search" data-table-search="apptTable" placeholder="Search appointments by advocate, date or legal topic…" aria-label="Search appointments">
            </div>
            <div class="tabs" data-tabs="apptTable">
                <button type="button" class="tab active" data-filter="ALL">All</button>
                <button type="button" class="tab" data-filter="PENDING">Pending</button>
                <button type="button" class="tab" data-filter="APPROVED">Approved</button>
                <button type="button" class="tab" data-filter="COMPLETED">Completed</button>
                <button type="button" class="tab" data-filter="CLOSED">Cancelled / rejected</button>
            </div>
            <div class="table-wrap">
                <table id="apptTable">
                    <tr>
                        <th>Advocate</th>
                        <th>Date &amp; time</th>
                        <th>Topic</th>
                        <th>Status</th>
                        <th>Actions</th>
                    </tr>
                    <%
                        boolean any = false;
                        try (Connection c = DB.get(); PreparedStatement s = c.prepareStatement(
                                "SELECT a.id,a.appointment_date,a.appointment_time,a.topic,a.details,a.status,a.advocate_note,u.name,p.specialisation,p.profile_photo,rv.rating,rv.comment "
                                + "FROM appointments a JOIN users u ON u.id=a.advocate_id LEFT JOIN advocate_profiles p ON p.user_id=u.id "
                                + "LEFT JOIN reviews rv ON rv.appointment_id=a.id WHERE a.client_id=? ORDER BY a.appointment_date DESC,a.appointment_time DESC")) {
                            s.setInt(1, myId);
                            try (ResultSet r = s.executeQuery()) {
                                while (r.next()) {
                                    any = true;
                                    String st = r.getString("status");
                                    String group = ("REJECTED".equals(st) || "CANCELLED".equals(st)) ? "CLOSED" : st;
                                    LocalDate apptDate = r.getDate("appointment_date").toLocalDate();
                                    boolean upcoming = !apptDate.isBefore(LocalDate.now());
                                    int rating = r.getInt("rating");
                                    boolean reviewed = !r.wasNull();
                                    String comment = r.getString("comment") == null ? "" : r.getString("comment");
                                    String note = r.getString("advocate_note");
                    %>
                    <tr data-group="<%=group%>">
                        <td>
                            <% String advocatePhoto = r.getString("profile_photo"); %>
                            <% if (advocatePhoto != null && !advocatePhoto.isEmpty()) { %><img class="advocate-photo mini-photo" src="<%=request.getContextPath()%>/<%=Util.esc(advocatePhoto)%>" alt=""><% } %>
                            <b><%=Util.esc(r.getString("name"))%></b><br>
                            <small class="muted"><%=Util.esc(r.getString("specialisation"))%></small>
                        </td>
                        <td>
                            <%=apptDate%><br>
                            <small class="muted"><%=r.getString("appointment_time").substring(0,5)%></small>
                        </td>
                        <td>
                            <%=Util.esc(r.getString("topic"))%><br>
                            <small class="muted"><%=Util.esc(r.getString("details"))%></small>
                            <% if (note != null && !note.isEmpty()) { %>
                                <div class="note">💬 Advocate: <%=Util.esc(note)%></div>
                            <% } %>
                            <% if (reviewed) { %>
                                <div class="note">⭐ Your review: <%=rating%>/5<%=comment.isEmpty() ? "" : " — " + Util.esc(comment)%></div>
                            <% } %>
                        </td>
                        <td><span class="status <%=st%>"><%=st%></span></td>
                        <td class="acts">
                            <% if ("PENDING".equals(st) && upcoming) { %>
                                <details>
                                    <summary class="tiny">Reschedule</summary>
                                    <form method="post" action="reschedule" class="mini">
                                        <input type="hidden" name="id" value="<%=r.getInt("id")%>">
                                        <input type="date" name="appointmentDate" required min="<%=LocalDate.now()%>" value="<%=apptDate%>">
                                        <input type="time" name="appointmentTime" required value="<%=r.getString("appointment_time").substring(0,5)%>">
                                        <button class="tiny">Save</button>
                                    </form>
                                </details>
                            <% } %>
                            <% if (("PENDING".equals(st) || "APPROVED".equals(st)) && upcoming) { %>
                                <form method="post" action="cancel-appointment" class="inline" onsubmit="return confirm('Cancel this appointment?');">
                                    <input type="hidden" name="id" value="<%=r.getInt("id")%>">
                                    <button class="tiny danger">Cancel</button>
                                </form>
                            <% } %>
                            <% if ("COMPLETED".equals(st)) { %>
                                <details>
                                    <summary class="tiny"><%=reviewed ? "Edit review" : "Rate advocate"%></summary>
                                    <form method="post" action="review-save" class="mini">
                                        <input type="hidden" name="id" value="<%=r.getInt("id")%>">
                                        <select name="rating" required>
                                            <% for (int i = 5; i >= 1; i--) { %>
                                                <option value="<%=i%>"<%=(reviewed && rating == i) ? " selected" : ""%>><%=i%> ★</option>
                                            <% } %>
                                        </select>
                                        <input name="comment" maxlength="500" placeholder="Short comment (optional)" value="<%=Util.esc(comment)%>">
                                        <button class="tiny">Submit</button>
                                    </form>
                                </details>
                            <% } %>
                        </td>
                    </tr>
                    <%
                                }
                            }
                        } catch (Exception e) {
                    %>
                    <tr><td colspan="5">Could not load appointments right now.</td></tr>
                    <%
                        }
                        if (!any) {
                    %>
                    <tr><td colspan="5" class="muted">No appointments yet. Use the form below or ask the AI legal guide which advocate suits your case.</td></tr>
                    <%
                        }
                    %>
                </table>
            </div>
        </section>

        <section class="panel" id="book">
            <h2>Book a consultation</h2>
            <form method="post" action="book" class="form-grid">
                <label>Advocate
                    <select name="advocateId" required>
                        <option value="">Select an advocate</option>
                        <%
                            try (Connection c = DB.get(); PreparedStatement s = c.prepareStatement(
                                    "SELECT u.id,u.name,p.specialisation,p.experience_years,p.city FROM users u LEFT JOIN advocate_profiles p ON p.user_id=u.id "
                                    + "WHERE u.role='ADVOCATE' AND u.status='ACTIVE' AND p.verification_status='APPROVED' ORDER BY u.name"); ResultSet r = s.executeQuery()) {
                                while (r.next()) {
                                    String sel = String.valueOf(r.getInt("id")).equals(pre) ? " selected" : "";
                                    String city = r.getString("city") == null || r.getString("city").isEmpty() ? "" : " · " + r.getString("city");
                        %>
                        <option value="<%=r.getInt("id")%>"<%=sel%>><%=Util.esc(r.getString("name"))%> — <%=Util.esc(r.getString("specialisation") == null ? "General Practice" : r.getString("specialisation"))%> (<%=r.getInt("experience_years")%> yrs<%=Util.esc(city)%>)</option>
                        <%
                                }
                            } catch (Exception e) { }
                        %>
                    </select>
                </label>
                <label>Legal topic
                    <input name="topic" required maxlength="160" list="topics" value="<%=Util.esc(preTopic)%>" placeholder="e.g. Property dispute, divorce">
                    <datalist id="topics">
                        <% for (LegalCategories.Cat cat : LegalCategories.ALL) { %>
                            <option value="<%=Util.esc(cat.label)%>">
                        <% } %>
                    </datalist>
                </label>
                <label>Date<input type="date" name="appointmentDate" required min="<%=LocalDate.now()%>"></label>
                <label>Time<input type="time" name="appointmentTime" required></label>
                <label class="wide">Brief description<textarea name="details" rows="3" maxlength="2000" placeholder="Share a short summary. Avoid sensitive details at this stage."></textarea></label>
                <button class="btn primary">Send request</button>
            </form>
        </section>
    </main>
    <%@ include file="includes/foot.jspf" %>
</body>
</html>
