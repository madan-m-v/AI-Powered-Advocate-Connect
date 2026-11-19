<%@ page contentType="text/html;charset=UTF-8" %>
<%
    // Opening the guide always starts a new conversation (the chat history lives in the session)
    session.removeAttribute("aiHistory");
    session.removeAttribute("aiAreas");
%>
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>AI legal guide | Advocate Connect</title>
    <link rel="stylesheet" href="assets/style.css">
</head>
<body>
    <%@ include file="includes/header.jspf" %>
    <main class="page narrow">
        <span class="eyebrow">AI-ASSISTED PREPARATION</span>
        <h1>Legal guide</h1>
        <p class="lead">Describe your problem, with or without legal keywords, to get a plain-language explanation (with a Gemini key), a preparation checklist and advocates who specialise in that kind of case. Then keep asking follow-up questions in the same chat.</p>
        <div class="notice">This tool provides general information only, not legal advice. Do not enter passwords, identity numbers, confidential evidence or highly sensitive personal information.</div>
        <section class="panel chat-panel">
            <div class="chips">
                <button type="button" class="chip" data-fill="I have a property dispute with my neighbour over land boundary">Property dispute</button>
                <button type="button" class="chip" data-fill="I want a divorce and custody of my child">Divorce &amp; custody</button>
                <button type="button" class="chip" data-fill="My cheque bounced and I need to send a legal notice">Cheque bounce</button>
                <button type="button" class="chip" data-fill="My employer terminated me and has not paid my salary">Job termination</button>
                <button type="button" class="chip" data-fill="Someone hacked my account and did online fraud">Cyber fraud</button>
                <button type="button" class="chip" data-fill="I need bail because a false FIR was filed against me">Police / FIR</button>
            </div>
            <div id="chatlog" class="chatlog">
                <div class="bubble bot">Hello. Tell me briefly what kind of legal issue you have, in your own words or with a few keywords. I will explain how to prepare and suggest advocates who handle that type of case. You can keep asking follow-up questions here.</div>
            </div>
            <form id="aiForm" data-role="<%=currentRole == null ? "" : currentRole%>">
                <textarea id="question" rows="3" required maxlength="2000" placeholder="Example: I have a property dispute with my brother over ancestral land..."></textarea>
                <div class="chat-actions">
                    <button type="button" id="aiReset" class="btn ghost small" hidden>Start a new chat</button>
                    <button id="aiSend" class="btn primary">Ask AI assistant →</button>
                </div>
            </form>
            <small id="aiState" class="muted">Keyword matching always works. With a Gemini API key in application.properties you also get a detailed explanation and follow-up answers, even when your question has no keywords.</small>
        </section>
    </main>
    <%@ include file="includes/foot.jspf" %>
</body>
</html>
