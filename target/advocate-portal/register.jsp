<%@ page contentType="text/html;charset=UTF-8" import="in.advocateportal.*" %>
<!DOCTYPE html>
<html>
<head>
    <title>Create account | Advocate Connect</title>
    <link rel="stylesheet" href="assets/style.css">
</head>
<body>
    <%@ include file="includes/header.jspf" %>
    <main class="auth-wrap">
        <section class="auth-card">
            <span class="eyebrow">GET STARTED</span>
            <h1>Create your account</h1>
            <p>Register as a client or an advocate.</p>
            <% if (request.getParameter("error") != null) { %>
                <div class="alert"><%=in.advocateportal.Util.esc(request.getParameter("error"))%></div>
            <% } %>
            <form method="post" action="register">
                <label>Full name</label>
                <input name="name" required maxlength="120">
                <label>Email</label>
                <input name="email" type="email" required>
                <label>Phone</label>
                <input name="phone" maxlength="20">
                <label>Account type</label>
                <select name="role" id="roleSelect">
                    <option value="CLIENT">Client</option>
                    <option value="ADVOCATE">Advocate</option>
                </select>
                <fieldset class="kyc-box" id="advocateFields" hidden>
                    <legend>Advocate verification (KYC)</legend>
                    <p class="muted">Required for KYC screening. An administrator reviews these details, and you appear to clients only after approval.</p>
                    <label>Bar Council enrolment number</label>
                    <input name="barNumber" id="barNumber" maxlength="40" placeholder="e.g. MAH/1234/2014" pattern="[A-Za-z0-9][A-Za-z0-9/\-. ]{2,39}" title="3 to 40 characters: letters, digits, spaces and / - .">
                    <label>Bar Council state</label>
                    <select name="barState" id="barState">
                        <option value="">Select your State Bar Council</option>
                        <% for (String st : BarCouncils.STATES) { %>
                            <option value="<%=Util.esc(st)%>"><%=Util.esc(st)%></option>
                        <% } %>
                    </select>
                    <label>Year of enrolment</label>
                    <input name="enrolmentYear" id="enrolmentYear" type="number" min="<%=BarCouncils.minYear()%>" max="<%=BarCouncils.maxYear()%>" placeholder="e.g. 2014">
                </fieldset>
                <label>Password (minimum 8 characters)</label>
                <input name="password" type="password" minlength="8" required>
                <button class="btn primary full">Create account</button>
            </form>
            <p class="muted">Already registered? <a href="login.jsp">Sign in</a></p>
        </section>
    </main>
    <%@ include file="includes/foot.jspf" %>
    <script>
        (function () {
            const role = document.getElementById('roleSelect');
            const box = document.getElementById('advocateFields');
            const fields = ['barNumber', 'barState', 'enrolmentYear'].map(id => document.getElementById(id));
            function sync() {
                const isAdvocate = role.value === 'ADVOCATE';
                box.hidden = !isAdvocate;
                fields.forEach(f => {
                    f.required = isAdvocate;
                    f.disabled = !isAdvocate;
                });
            }
            role.addEventListener('change', sync);
            sync();
        })();
    </script>
</body>
</html>
