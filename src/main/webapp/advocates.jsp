<%@ page contentType="text/html;charset=UTF-8" import="java.sql.*,java.util.ArrayList,java.util.List,in.advocateportal.*" %>
<%
    request.setCharacterEncoding("UTF-8");
    String kw = request.getParameter("q") == null ? "" : request.getParameter("q").trim();
    String specFilter = request.getParameter("spec") == null ? "" : request.getParameter("spec").trim();
    String cityFilter = request.getParameter("city") == null ? "" : request.getParameter("city").trim();
    String sort = request.getParameter("sort") == null ? "" : request.getParameter("sort");
    if (kw.length() > 100) {
        kw = kw.substring(0, 100);
    }
%>
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Find advocates | Advocate Connect</title>
    <link rel="stylesheet" href="assets/style.css">
</head>
<body>
    <%@ include file="includes/header.jspf" %>
    <main class="page">
        <span class="eyebrow">OUR NETWORK</span>
        <h1>Find an advocate</h1>
        <p class="lead">Search by name or describe your case (for example “property dispute” or “divorce”), or filter by practice area and city.</p>
        <form method="get" class="filter-bar">
            <input name="q" value="<%=Util.esc(kw)%>" placeholder="Search name, case type or keyword" maxlength="100">
            <select name="spec">
                <option value="">All practice areas</option>
                <% for (LegalCategories.Cat cat : LegalCategories.ALL) { %>
                    <option value="<%=Util.esc(cat.label)%>"<%=cat.label.equalsIgnoreCase(specFilter) ? " selected" : ""%>><%=Util.esc(cat.label)%></option>
                <% } %>
            </select>
            <select name="city">
                <option value="">All cities</option>
                <%
                    try (Connection c = DB.get(); PreparedStatement s = c.prepareStatement("SELECT DISTINCT p.city FROM advocate_profiles p JOIN users u ON u.id=p.user_id WHERE u.status='ACTIVE' AND p.verification_status='APPROVED' AND p.city IS NOT NULL AND p.city<>'' ORDER BY p.city"); ResultSet r = s.executeQuery()) {
                        while (r.next()) {
                %>
                            <option value="<%=Util.esc(r.getString(1))%>"<%=r.getString(1).equalsIgnoreCase(cityFilter) ? " selected" : ""%>><%=Util.esc(r.getString(1))%></option>
                <%
                        }
                    } catch (Exception e) { }
                %>
            </select>
            <select name="sort">
                <option value="">Sort: most experienced</option>
                <option value="rating"<%="rating".equals(sort) ? " selected" : ""%>>Sort: top rated</option>
                <option value="fee"<%="fee".equals(sort) ? " selected" : ""%>>Sort: lowest fee</option>
            </select>
            <button class="btn primary">Search</button>
            <% if (!kw.isEmpty() || !specFilter.isEmpty() || !cityFilter.isEmpty() || !sort.isEmpty()) { %>
                <a class="btn ghost" href="advocates.jsp">Clear</a>
            <% } %>
        </form>
        <div class="profile-grid">
            <%
                StringBuilder sql = new StringBuilder(
                        "SELECT u.id,u.name,p.specialisation,p.experience_years,p.bio,p.city,p.languages,p.fee,p.profile_photo,COALESCE(rv.avg_r,0) avg_r,COALESCE(rv.n,0) n "
                        + "FROM users u LEFT JOIN advocate_profiles p ON p.user_id=u.id "
                        + "LEFT JOIN (SELECT advocate_id,AVG(rating) avg_r,COUNT(*) n FROM reviews GROUP BY advocate_id) rv ON rv.advocate_id=u.id "
                        + "WHERE u.role='ADVOCATE' AND u.status='ACTIVE' AND p.verification_status='APPROVED'");
                List<String> params = new ArrayList<>();
                if (!kw.isEmpty()) {
                    sql.append(" AND (u.name LIKE ? OR p.bio LIKE ? OR p.specialisation LIKE ?");
                    params.add("%" + kw + "%");
                    params.add("%" + kw + "%");
                    params.add("%" + kw + "%");
                    for (LegalCategories.Cat cat : LegalCategories.detect(kw, 2)) {
                        sql.append(" OR p.specialisation LIKE ?");
                        params.add("%" + cat.label + "%");
                    }
                    sql.append(")");
                }
                if (!specFilter.isEmpty()) {
                    sql.append(" AND p.specialisation LIKE ?");
                    params.add("%" + specFilter + "%");
                }
                if (!cityFilter.isEmpty()) {
                    sql.append(" AND p.city = ?");
                    params.add(cityFilter);
                }
                if ("rating".equals(sort)) {
                    sql.append(" ORDER BY avg_r DESC, n DESC, p.experience_years DESC, u.name");
                } else if ("fee".equals(sort)) {
                    sql.append(" ORDER BY (p.fee IS NULL OR p.fee=0), p.fee ASC, u.name");
                } else {
                    sql.append(" ORDER BY p.experience_years DESC, u.name");
                }
                boolean any = false;
                Object roleNow = session.getAttribute("role");
                try (Connection c = DB.get(); PreparedStatement s = c.prepareStatement(sql.toString())) {
                    for (int i = 0; i < params.size(); i++) {
                        s.setString(i + 1, params.get(i));
                    }
                    try (ResultSet r = s.executeQuery()) {
                        while (r.next()) {
                            any = true;
                            String name = r.getString("name"), specs = r.getString("specialisation") == null ? "General Practice" : r.getString("specialisation");
                            String bioTxt = r.getString("bio") == null || r.getString("bio").isEmpty() ? "Advocate profile. Request a consultation to discuss your case." : r.getString("bio");
                            int reviews = r.getInt("n"), fee = r.getInt("fee");
                            String city = r.getString("city") == null ? "" : r.getString("city"), langs = r.getString("languages") == null ? "" : r.getString("languages");
            %>
            <article class="profile">
                <% String photo = r.getString("profile_photo"); %>
                <% if (photo != null && !photo.isEmpty()) { %><img class="advocate-photo" src="<%=request.getContextPath()%>/<%=Util.esc(photo)%>" alt="Photo of <%=Util.esc(name)%>" loading="lazy"><% } else { %><div class="avatar"><%=Util.esc(name.substring(0, 1).toUpperCase())%></div><% } %>
                <h3><%=Util.esc(name)%></h3>
                <div class="verified-badge" title="Bar Council enrolment verified by an administrator">✔ Verified advocate</div>
                <div class="rating"><%=reviews > 0 ? "★ " + String.format(java.util.Locale.ROOT, "%.1f", r.getDouble("avg_r")) + " (" + reviews + " review" + (reviews == 1 ? "" : "s") + ")" : "New on Advocate Connect"%></div>
                <div class="tags">
                    <% for (String sp : specs.split(",")) { %>
                        <span class="tag"><%=Util.esc(sp.trim())%></span>
                    <% } %>
                </div>
                <p><%=Util.esc(bioTxt)%></p>
                <small>
                    <%=r.getInt("experience_years")%> years experience<%=city.isEmpty() ? "" : " · " + Util.esc(city)%><br>
                    <%=langs.isEmpty() ? "" : Util.esc(langs) + "<br>"%>
                    <b><%=fee > 0 ? "₹" + fee + " per consultation" : "Fee on request"%></b>
                </small>
                <% if ("CLIENT".equals(roleNow)) { %>
                    <a class="btn primary full" href="client.jsp?advocate=<%=r.getInt("id")%>#book">Request consultation</a>
                <% } else if (roleNow == null) { %>
                    <a class="btn primary full" href="login.jsp">Sign in to request</a>
                <% } %>
            </article>
            <%
                        }
                    }
                } catch (Exception e) {
            %>
                <p>Profiles are being prepared. Please try again later.</p>
            <%
                }
                if (!any) {
            %>
                <p class="muted">No advocates match your search yet. Try a different keyword or clear the filters.</p>
            <%
                }
            %>
        </div>
    </main>
    <%@ include file="includes/foot.jspf" %>
</body>
</html>
