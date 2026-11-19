<%@ page contentType="text/html;charset=UTF-8" import="java.sql.*,in.advocateportal.*" %>
<%
    if (session.getAttribute("uid") == null) {
        response.sendRedirect("login.jsp");
        return;
    }
    int myId = (Integer) session.getAttribute("uid");
    String name = "", email = "", phone = "", role = "";
    try (Connection c = DB.get(); PreparedStatement s = c.prepareStatement("SELECT name,email,phone,role FROM users WHERE id=?")) {
        s.setInt(1, myId);
        try (ResultSet r = s.executeQuery()) {
            if (r.next()) {
                name = r.getString(1);
                email = r.getString(2);
                phone = r.getString(3) == null ? "" : r.getString(3);
                role = r.getString(4);
            }
        }
    } catch (Exception e) { }
%>
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>My account | Advocate Connect</title>
    <link rel="stylesheet" href="assets/style.css">
</head>
<body>
    <%@ include file="includes/header.jspf" %>
    <main class="page narrow">
        <span class="eyebrow"><%=Util.esc(role)%> ACCOUNT</span>
        <h1>My account</h1>
        <%=Util.flashHtml(request.getParameter("msg"))%>
        <section class="panel">
            <h2>Personal details</h2>
            <form method="post" action="account-save" class="form-grid">
                <label>Full name<input name="name" required maxlength="120" value="<%=Util.esc(name)%>"></label>
                <label>Phone<input name="phone" maxlength="20" value="<%=Util.esc(phone)%>"></label>
                <label class="wide">Email (cannot be changed)<input value="<%=Util.esc(email)%>" disabled></label>
                <button class="btn primary">Save details</button>
            </form>
        </section>
        <section class="panel">
            <h2>Change password</h2>
            <form method="post" action="password-change" class="form-grid">
                <label class="wide">Current password<input type="password" name="current" required></label>
                <label>New password (8+ characters)<input type="password" name="newPassword" minlength="8" maxlength="100" required></label>
                <label>Confirm new password<input type="password" name="confirm" minlength="8" maxlength="100" required></label>
                <button class="btn primary">Change password</button>
            </form>
        </section>
    </main>
    <%@ include file="includes/foot.jspf" %>
</body>
</html>
