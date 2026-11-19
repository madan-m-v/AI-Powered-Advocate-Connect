<%@ page contentType="text/html;charset=UTF-8" import="java.sql.*,in.advocateportal.*" %>
<%
    if (!"ADMIN".equals(session.getAttribute("role"))) {
        response.sendRedirect("login.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Admin dashboard | Advocate Connect</title>
    <link rel="stylesheet" href="assets/style.css">
</head>
<body>
    <%@ include file="includes/header.jspf" %>
    <main class="page">
        <div class="dash-head">
            <div>
                <span class="eyebrow">PLATFORM CONTROL CENTRE</span>
                <h1>Admin dashboard</h1>
                <p class="lead">A clear view of people, consultations and platform activity.</p>
            </div>
            <a class="btn ghost" href="dashboard">Refresh overview ↻</a>
        </div>
        <%=Util.flashHtml(request.getParameter("msg"))%>
        <%
            int users = 0, clients = 0, advocates = 0, disabled = 0, appointments = 0, pending = 0, approved = 0, completed = 0, unverified = 0;
            try (Connection c = DB.get(); Statement s = c.createStatement()) {
                try (ResultSet r = s.executeQuery("SELECT COUNT(*),SUM(role='CLIENT'),SUM(role='ADVOCATE'),SUM(status='DISABLED') FROM users")) {
                    if (r.next()) {
                        users = r.getInt(1);
                        clients = r.getInt(2);
                        advocates = r.getInt(3);
                        disabled = r.getInt(4);
                    }
                }
                try (ResultSet r = s.executeQuery("SELECT COUNT(*) FROM advocate_profiles WHERE verification_status='PENDING'")) {
                    if (r.next()) {
                        unverified = r.getInt(1);
                    }
                }
                try (ResultSet r = s.executeQuery("SELECT COUNT(*),SUM(status='PENDING'),SUM(status='APPROVED'),SUM(status='COMPLETED') FROM appointments")) {
                    if (r.next()) {
                        appointments = r.getInt(1);
                        pending = r.getInt(2);
                        approved = r.getInt(3);
                        completed = r.getInt(4);
                    }
                }
            } catch (Exception e) { }
        %>
        <div class="stats admin-stats">
            <div><small>Total users</small><strong><%=users%></strong><span class="metric-note">All registered accounts</span></div>
            <div><small>Clients</small><strong><%=clients%></strong><span class="metric-note">Client accounts</span></div>
            <div><small>Advocates</small><strong><%=advocates%></strong><span class="metric-note"><%=unverified%> awaiting verification</span></div>
            <div><small>Appointments</small><strong><%=appointments%></strong><span class="metric-note"><%=pending%> awaiting response</span></div>
            <div><small>Approved</small><strong><%=approved%></strong><span class="metric-note">Scheduled consultations</span></div>
            <div><small>Disabled users</small><strong><%=disabled%></strong><span class="metric-note">Accounts currently restricted</span></div>
        </div>

        <section class="panel" id="verification">
            <div class="section-title-row">
                <div>
                    <span class="eyebrow">KYC SCREENING</span>
                    <h2>Advocate verification</h2>
                    <p class="muted">Check each advocate's Bar Council details. Only approved advocates appear to clients, in the directory, the booking form and AI suggestions.</p>
                </div>
            </div>
            <div class="filter-bar">
                <input type="search" id="kycSearch" placeholder="Search name, email, enrolment number or state…" aria-label="Search advocates">
                <select id="kycStatusFilter">
                    <option value="">All verification states</option>
                    <option<%="PENDING".equals(request.getParameter("kyc")) || request.getParameter("kyc") == null ? " selected" : ""%>>PENDING</option>
                    <option>APPROVED</option>
                    <option>REJECTED</option>
                </select>
            </div>
            <div class="table-wrap">
                <table id="kycTable">
                    <thead>
                        <tr>
                            <th>Advocate</th>
                            <th>Bar Council enrolment no.</th>
                            <th>Bar Council state</th>
                            <th>Year</th>
                            <th>Verification</th>
                            <th>Decision</th>
                        </tr>
                    </thead>
                    <tbody>
                        <%
                            try (Connection c = DB.get(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(
                                    "SELECT u.id,u.name,u.email,u.phone,u.status,u.created_at,p.bar_council_number,p.bar_council_state,p.enrolment_year,"
                                    + "p.verification_status,p.verification_note,p.verified_at,p.profile_photo "
                                    + "FROM users u LEFT JOIN advocate_profiles p ON p.user_id=u.id WHERE u.role='ADVOCATE' "
                                    + "ORDER BY FIELD(COALESCE(p.verification_status,'PENDING'),'PENDING','REJECTED','APPROVED'),u.created_at DESC")) {
                                while (r.next()) {
                                    String vs = r.getString("verification_status") == null ? "PENDING" : r.getString("verification_status");
                                    String num = r.getString("bar_council_number") == null ? "" : r.getString("bar_council_number");
                                    String st = r.getString("bar_council_state") == null ? "" : r.getString("bar_council_state");
                                    int yr = r.getInt("enrolment_year");
                                    String vnote = r.getString("verification_note") == null ? "" : r.getString("verification_note");
                        %>
                        <tr data-status="<%=Util.esc(vs)%>">
                            <td>
                                <% String adminPhoto = r.getString("profile_photo"); %>
                                <% if (adminPhoto != null && !adminPhoto.isEmpty()) { %><img class="advocate-photo mini-photo" src="<%=request.getContextPath()%>/<%=Util.esc(adminPhoto)%>" alt=""><% } %>
                                <b><%=Util.esc(r.getString("name"))%></b><br>
                                <small class="muted"><%=Util.esc(r.getString("email"))%></small><br>
                                <small class="muted"><%=Util.esc(r.getString("phone"))%></small>
                            </td>
                            <td><%=num.isEmpty() ? "<small class=\"muted\">Not provided</small>" : "<b>" + Util.esc(num) + "</b>"%></td>
                            <td><%=st.isEmpty() ? "<small class=\"muted\">Not provided</small>" : Util.esc(st)%></td>
                            <td><%=yr > 0 ? String.valueOf(yr) : "—"%></td>
                            <td>
                                <span class="status <%=Util.esc(vs)%>"><%="APPROVED".equals(vs) ? "VERIFIED" : Util.esc(vs)%></span>
                                <% if (!vnote.isEmpty()) { %>
                                    <br><small class="muted"><%=Util.esc(vnote)%></small>
                                <% } %>
                            </td>
                            <td>
                                <form method="post" action="admin-action" class="verify-form">
                                    <input type="hidden" name="id" value="<%=r.getInt("id")%>">
                                    <input name="note" maxlength="300" placeholder="Note (optional, shown to advocate)" aria-label="Verification note">
                                    <div class="verify-actions">
                                        <% if (!"APPROVED".equals(vs)) { %>
                                            <button class="tiny" name="action" value="approve-advocate" onclick="return confirm('Approve this advocate? They will become visible to clients.');">Approve</button>
                                        <% } %>
                                        <% if (!"REJECTED".equals(vs)) { %>
                                            <button class="tiny danger" name="action" value="reject-advocate" onclick="return confirm('Reject this advocate? They will be hidden from clients.');"><%="APPROVED".equals(vs) ? "Revoke" : "Reject"%></button>
                                        <% } %>
                                    </div>
                                </form>
                            </td>
                        </tr>
                        <%
                                }
                            } catch (Exception e) {
                        %>
                        <tr><td colspan="6">Unable to load advocate verification details. Check the database connection and that the latest SQL was imported.</td></tr>
                        <%
                            }
                        %>
                    </tbody>
                </table>
                <p id="kycEmpty" class="muted" hidden>No advocates in this view. Change the verification filter to see others.</p>
            </div>
        </section>

        <section class="panel">
            <div class="section-title-row">
                <div>
                    <span class="eyebrow">ACCOUNT DIRECTORY</span>
                    <h2>User management</h2>
                    <p class="muted">Search accounts and enable or disable access. Admin accounts are protected.</p>
                </div>
            </div>
            <div class="filter-bar">
                <input type="search" id="userSearch" placeholder="Search name, email or role…" aria-label="Search users">
                <select id="roleFilter">
                    <option value="">All roles</option>
                    <option>CLIENT</option>
                    <option>ADVOCATE</option>
                    <option>ADMIN</option>
                </select>
                <select id="userStatusFilter">
                    <option value="">All account states</option>
                    <option>ACTIVE</option>
                    <option>DISABLED</option>
                </select>
            </div>
            <div class="table-wrap">
                <table id="usersTable">
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Email</th>
                            <th>Role</th>
                            <th>Account status</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        <%
                            try (Connection c = DB.get(); Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT id,name,email,role,status FROM users ORDER BY created_at DESC")) {
                                while (r.next()) {
                                    String role = r.getString("role"), status = r.getString("status");
                        %>
                        <tr data-role="<%=Util.esc(role)%>" data-status="<%=Util.esc(status)%>">
                            <td><b><%=Util.esc(r.getString("name"))%></b></td>
                            <td><%=Util.esc(r.getString("email"))%></td>
                            <td><span class="tag"><%=Util.esc(role)%></span></td>
                            <td><span class="status <%=Util.esc(status)%>"><%=Util.esc(status)%></span></td>
                            <td>
                                <% if (!"ADMIN".equals(role)) { %>
                                    <form method="post" action="admin-action" class="inline" onsubmit="return confirm('Change access for this account?');">
                                        <input type="hidden" name="id" value="<%=r.getInt("id")%>">
                                        <button class="tiny <%= "ACTIVE".equals(status) ? "danger" : "" %>" name="action" value="<%= "ACTIVE".equals(status) ? "disable" : "enable" %>"><%= "ACTIVE".equals(status) ? "Disable" : "Enable" %></button>
                                    </form>
                                <% } else { %>
                                    <small class="muted">Protected</small>
                                <% } %>
                            </td>
                        </tr>
                        <%
                                }
                            } catch (Exception e) {
                        %>
                        <tr><td colspan="5">Unable to load user accounts. Check the database connection.</td></tr>
                        <%
                            }
                        %>
                    </tbody>
                </table>
                <p id="userEmpty" class="muted" hidden>No users match your search.</p>
            </div>
        </section>

        <section class="panel">
            <span class="eyebrow">SERVICE ACTIVITY</span>
            <h2>Appointment overview</h2>
            <div class="stats compact-stats">
                <div><small>Pending</small><strong><%=pending%></strong></div>
                <div><small>Approved</small><strong><%=approved%></strong></div>
                <div><small>Completed</small><strong><%=completed%></strong></div>
            </div>
            <div class="filter-bar">
                <input type="search" id="apptSearch" placeholder="Search client, advocate or legal topic…" aria-label="Search appointments">
                <select id="apptStatusFilter">
                    <option value="">All appointment statuses</option>
                    <option>PENDING</option>
                    <option>APPROVED</option>
                    <option>COMPLETED</option>
                    <option>REJECTED</option>
                    <option>CANCELLED</option>
                </select>
            </div>
            <div class="table-wrap">
                <table id="appointmentsTable">
                    <thead>
                        <tr>
                            <th>Client</th>
                            <th>Advocate</th>
                            <th>Legal topic</th>
                            <th>Date &amp; time</th>
                            <th>Status</th>
                        </tr>
                    </thead>
                    <tbody>
                        <%
                            try (Connection c = DB.get(); Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT c.name client,a.name advocate,x.topic,x.appointment_date,x.appointment_time,x.status FROM appointments x JOIN users c ON c.id=x.client_id JOIN users a ON a.id=x.advocate_id ORDER BY x.created_at DESC LIMIT 300")) {
                                while (r.next()) {
                                    String st = r.getString("status");
                        %>
                        <tr data-status="<%=Util.esc(st)%>">
                            <td><%=Util.esc(r.getString("client"))%></td>
                            <td><%=Util.esc(r.getString("advocate"))%></td>
                            <td><%=Util.esc(r.getString("topic"))%></td>
                            <td>
                                <%=r.getDate("appointment_date")%><br>
                                <small class="muted"><%=r.getString("appointment_time").substring(0,5)%></small>
                            </td>
                            <td><span class="status <%=Util.esc(st)%>"><%=Util.esc(st)%></span></td>
                        </tr>
                        <%
                                }
                            } catch (Exception e) {
                        %>
                        <tr><td colspan="5">Unable to load appointment activity.</td></tr>
                        <%
                            }
                        %>
                    </tbody>
                </table>
                <p id="apptEmpty" class="muted" hidden>No appointments match your search.</p>
            </div>
        </section>
        <p class="muted admin-footnote">Privacy note: review account details only for platform administration. Appointment content should be treated as confidential.</p>
    </main>
    <script>
        (function () {
            function filter(tableId, searchId, statusId, roleId, emptyId) {
                const table = document.getElementById(tableId);
                const search = document.getElementById(searchId);
                const status = document.getElementById(statusId);
                const role = roleId ? document.getElementById(roleId) : null;
                const empty = document.getElementById(emptyId);
                if (!table) {
                    return;
                }
                function apply() {
                    let shown = 0;
                    table.querySelectorAll('tbody tr').forEach(row => {
                        const txt = row.innerText.toLowerCase();
                        const ok = (!search || txt.includes(search.value.toLowerCase()))
                                && (!status || !status.value || row.dataset.status === status.value)
                                && (!role || !role.value || row.dataset.role === role.value);
                        row.hidden = !ok;
                        if (ok) {
                            shown++;
                        }
                    });
                    if (empty) {
                        empty.hidden = shown !== 0;
                    }
                }
                [search, status, role].filter(Boolean).forEach(el => el.addEventListener('input', apply));
                apply();
            }
            filter('kycTable', 'kycSearch', 'kycStatusFilter', null, 'kycEmpty');
            filter('usersTable', 'userSearch', 'userStatusFilter', 'roleFilter', 'userEmpty');
            filter('appointmentsTable', 'apptSearch', 'apptStatusFilter', null, 'apptEmpty');
        })();
    </script>
    <%@ include file="includes/foot.jspf" %>
</body>
</html>
