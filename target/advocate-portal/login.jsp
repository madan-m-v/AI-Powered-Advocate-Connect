<!DOCTYPE html>
<html>
<head>
    <title>Sign in | Advocate Connect</title>
    <link rel="stylesheet" href="assets/style.css">
</head>
<body>
    <%@ include file="includes/header.jspf" %>
    <main class="auth-wrap">
        <section class="auth-card">
            <span class="eyebrow">WELCOME BACK</span>
            <h1>Sign in to your desk</h1>
            <p>Access your appointments and legal workspace.</p>
            <% if (request.getParameter("error") != null) { %>
                <div class="alert"><%=in.advocateportal.Util.esc(request.getParameter("error"))%></div>
            <% } %>
            <% if (request.getParameter("registered") != null) { %>
                <% if ("advocate".equals(request.getParameter("registered"))) { %>
                    <div class="success">Account created. Your Bar Council details are now with our administrators for KYC review. You can sign in now, but clients will see you only after approval.</div>
                <% } else { %>
                    <div class="success">Account created. Please sign in.</div>
                <% } %>
            <% } %>
            <form method="post" action="login">
                <label>Email address</label>
                <input name="email" type="email" required placeholder="you@example.com">
                <label>Password</label>
                <input name="password" type="password" required>
                <button class="btn primary full">Sign in</button>
            </form>
            <p class="muted">New to Advocate Connect? <a href="register.jsp">Create an account</a></p>
        </section>
    </main>
    <%@ include file="includes/foot.jspf" %>
</body>
</html>
