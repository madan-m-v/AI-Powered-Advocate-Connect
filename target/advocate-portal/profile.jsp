<%@ page contentType="text/html;charset=UTF-8" import="java.sql.*,in.advocateportal.*" %>
<%
    if (!"ADVOCATE".equals(session.getAttribute("role"))) {
        response.sendRedirect("login.jsp");
        return;
    }
    int myId = (Integer) session.getAttribute("uid");
    String spec = "", bio = "", city = "", languages = "", profilePhoto = "";
    String barNumber = "", barState = "", vStatus = "PENDING", vNote = "";
    int years = 0, fee = 0, enrolYear = 0;
    try (Connection c = DB.get(); PreparedStatement s = c.prepareStatement(
            "SELECT specialisation,experience_years,bio,city,languages,fee,bar_council_number,bar_council_state,enrolment_year,"
            + "verification_status,verification_note,profile_photo FROM advocate_profiles WHERE user_id=?")) {
        s.setInt(1, myId);
        try (ResultSet r = s.executeQuery()) {
            if (r.next()) {
                spec = r.getString(1) == null ? "" : r.getString(1);
                years = r.getInt(2);
                bio = r.getString(3) == null ? "" : r.getString(3);
                city = r.getString(4) == null ? "" : r.getString(4);
                languages = r.getString(5) == null ? "" : r.getString(5);
                fee = r.getInt(6);
                barNumber = r.getString(7) == null ? "" : r.getString(7);
                barState = r.getString(8) == null ? "" : r.getString(8);
                enrolYear = r.getInt(9);
                vStatus = r.getString(10) == null ? "PENDING" : r.getString(10);
                vNote = r.getString(11) == null ? "" : r.getString(11);
                profilePhoto = r.getString(12) == null ? "" : r.getString(12);
            }
        }
    } catch (Exception e) { }
    String specLower = spec.toLowerCase();
    boolean locked = "APPROVED".equals(vStatus);
%>
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>My profile | Advocate Connect</title>
    <link rel="stylesheet" href="assets/style.css">
</head>
<body>
    <%@ include file="includes/header.jspf" %>
    <main class="page narrow">
        <span class="eyebrow">ADVOCATE PROFILE</span>
        <h1>Your public profile</h1>
        <div class="advocate-photo-editor">
            <% if (!profilePhoto.isEmpty()) { %><img class="advocate-photo large" src="<%=request.getContextPath()%>/<%=Util.esc(profilePhoto)%>" alt="Your profile photo"><% } else { %><div class="avatar avatar-large"><%=Util.esc(((String)session.getAttribute("name")).substring(0,1).toUpperCase())%></div><% } %>
            <p class="muted">Your photo appears in the advocate directory and client appointment views.</p>
        </div>
        <p class="lead">Clients see this in the directory. The AI legal guide uses your practice areas to suggest you for matching cases.</p>
        <%=Util.flashHtml(request.getParameter("msg"))%>
        <section class="panel" id="verification">
            <span class="eyebrow">KYC VERIFICATION</span>
            <h2>Bar Council details <span class="status <%=Util.esc(vStatus)%>"><%="APPROVED".equals(vStatus) ? "VERIFIED" : Util.esc(vStatus)%></span></h2>
            <% if ("APPROVED".equals(vStatus)) { %>
                <p class="muted">An administrator verified your details. You are visible to clients. To change these details, contact an administrator.</p>
            <% } else if ("REJECTED".equals(vStatus)) { %>
                <div class="alert">Your verification was rejected<%=vNote.isEmpty() ? "." : ": " + Util.esc(vNote)%> Correct the details below and submit them again.</div>
            <% } else { %>
                <p class="muted">Your details are awaiting administrator review. Clients cannot see or book you until you are approved.</p>
            <% } %>
            <form method="post" action="verification-save">
                <div class="form-grid">
                    <label>Bar Council enrolment number
                        <input name="barNumber" required maxlength="40" value="<%=Util.esc(barNumber)%>" placeholder="e.g. MAH/1234/2014" pattern="[A-Za-z0-9][A-Za-z0-9/\-. ]{2,39}" title="3 to 40 characters: letters, digits, spaces and / - ."<%=locked ? " readonly" : ""%>>
                    </label>
                    <label>Bar Council state
                        <select name="barState" required<%=locked ? " disabled" : ""%>>
                            <option value="">Select your State Bar Council</option>
                            <% for (String st : BarCouncils.STATES) { %>
                                <option value="<%=Util.esc(st)%>"<%=st.equals(barState) ? " selected" : ""%>><%=Util.esc(st)%></option>
                            <% } %>
                        </select>
                    </label>
                    <label>Year of enrolment
                        <input name="enrolmentYear" type="number" required min="<%=BarCouncils.minYear()%>" max="<%=BarCouncils.maxYear()%>" value="<%=enrolYear > 0 ? String.valueOf(enrolYear) : ""%>" placeholder="e.g. 2014"<%=locked ? " readonly" : ""%>>
                    </label>
                </div>
                <% if (!locked) { %>
                    <div class="actions">
                        <button class="btn primary">Submit for verification</button>
                    </div>
                <% } %>
            </form>
        </section>
        <section class="panel">
            <form method="post" action="profile-save" enctype="multipart/form-data">
                <label>Profile photo <small class="muted">(JPG, PNG or WEBP; maximum 2 MB)</small><input type="file" name="profilePhoto" accept="image/jpeg,image/png,image/webp"></label>
                <label>Practice areas <small class="muted">(choose up to 4)</small></label>
                <div class="checks" id="specChecks">
                    <% for (LegalCategories.Cat cat : LegalCategories.ALL) { %>
                        <label class="check"><input type="checkbox" name="spec" value="<%=Util.esc(cat.label)%>"<%=specLower.contains(cat.label.toLowerCase()) ? " checked" : ""%>> <span><%=Util.esc(cat.label)%></span></label>
                    <% } %>
                </div>
                <div class="form-grid">
                    <label>Years of experience<input type="number" name="experience" min="0" max="60" value="<%=years%>"></label>
                    <label>Consultation fee (₹)<input type="number" name="fee" min="0" max="1000000" value="<%=fee%>"></label>
                    <label>City<input name="city" maxlength="80" value="<%=Util.esc(city)%>" placeholder="e.g. Mumbai"></label>
                    <label>Languages<input name="languages" maxlength="120" value="<%=Util.esc(languages)%>" placeholder="e.g. English, Hindi"></label>
                </div>
                <label>About you<textarea name="bio" rows="5" maxlength="1000" placeholder="Courts you appear in, types of cases you handle..."><%=Util.esc(bio)%></textarea></label>
                <div class="actions">
                    <button class="btn primary">Save profile</button>
                    <a class="btn ghost" href="dashboard">Back to dashboard</a>
                </div>
            </form>
        </section>
    </main>
    <%@ include file="includes/foot.jspf" %>
</body>
</html>
