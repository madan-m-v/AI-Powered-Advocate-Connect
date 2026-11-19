# Advocate Connect — AI-Based Advocate Search & Booking Portal

Java Servlets + JSP + JDBC + MySQL 8, packaged as a Maven WAR for **Tomcat 9** (`javax.servlet`) and **JDK 11**, ready for Apache NetBeans.

## Requirements
- JDK 11, Apache NetBeans (Maven support), Apache Tomcat **9.x** (not 10+), MySQL 8+
- Internet on the first build so Maven can download dependencies

## 1. Database — one importable SQL file

**Use only `database/advocate_connect_all_in_one.sql`** for a fresh/demo installation. The previous separate schema, verification migration, profile-photo migration, and demo-data scripts have been consolidated into this single file.

1. Start **MySQL 8.0+**.
2. In MySQL Workbench, choose **File → Open SQL Script**, select `database/advocate_connect_all_in_one.sql`, then execute the complete script (lightning icon).
3. The script creates `advocate_portal`, all four application tables (`users`, `advocate_profiles`, `appointments`, `reviews`), indexes, foreign keys, profile-photo and advocate-verification columns, and demo accounts.
4. In `src/main/resources/application.properties`, set `db.user`, `db.password`, and (if needed) `db.url`. Environment variables `DB_URL`, `DB_USER`, and `DB_PASSWORD` override these values.
5. Clean and Build, then run on **Tomcat 9.x** with **JDK 11**.

**Important:** this is a fresh/demo installer and drops/recreates the four application tables. Back up existing data before importing. Do not run it against a live database containing data you need to keep. The split SQL scripts are intentionally no longer included, to avoid running migrations twice or in the wrong order.

The installer includes demo users so you can test all three roles immediately. Demo passwords are stored as plain text for this academic project; do not reuse real passwords or deploy with real user data until password hashing is implemented.

## 2. Gemini API key (optional, for legal explanations)
1. Create a key at https://aistudio.google.com/apikey
2. Open `src/main/resources/application.properties` and set `gemini.api.key=YOUR_KEY` (or set the `GEMINI_API_KEY` environment variable).
3. Clean and Build, then run again. The server needs internet access.

The AI legal guide works in three ways, alone or together:
- **Keyword matching** (no key needed) detects the practice area, lists documents and questions, and suggests advocates.
- **Gemini** (with a key) writes a plain-language explanation. It works **with or without keywords**: if no keyword matches,
  Gemini still answers the question and names the practice area, so advocate suggestions and the checklist still appear.
- **Keyword suggestions**: when the guide cannot match your question (or to refine a match) it shows tappable keyword chips
  such as `+ boundary dispute`. Tapping one adds it to your question.

The chat **continues across questions**. The conversation is kept in your login session and sent to Gemini with every follow-up
(for example "what documents do I need?"), so answers stay in context. Follow-ups on the same topic return just the answer;
a new topic shows the checklist and advocates again. Use **Start a new chat** (or reopen the page) to begin again.
Without a key, only keyword matching and keyword suggestions are available. If Gemini fails, the page shows the reason
(bad key, quota, safety filter, no internet) and the keyword answer is still returned.

## 3. Run from NetBeans
1. File > Open Project > select `advocate-booking-portal` (Maven icon).
2. Tools > Servers > Add Server > Apache Tomcat > select your Tomcat 9 folder.
3. Project Properties > Run > Server = Apache Tomcat 9.
4. Right-click project > Clean and Build, then Run.
5. Open http://localhost:8080/advocate-portal/

Without NetBeans: `mvn clean package`, copy `target/advocate-portal.war` to `<tomcat>/webapps/`, run `bin/startup.bat`.

## 4. Logins (with demo_data.sql)
- Admin: `admin@advocateconnect.local` / `Admin@123`
- Client: `client@advocateconnect.local` / `Demo@123`
- Demo advocates are pre-approved. To try the approval flow, register a new advocate and approve them as admin.
- Advocates (`Demo@123`): `aarav@` property, `priya@` family/divorce, `rohan@` criminal, `sneha@` cheque bounce,
  `karan@` employment, `nisha@` cyber (all `@advocateconnect.local`)

Without demo data, register at `/register.jsp` and make an admin with
`UPDATE users SET role='ADMIN' WHERE email='you@example.com';`

## Advocate verification (KYC) workflow
1. **Register:** choosing *Advocate* on `/register.jsp` reveals three required fields: **Bar Council enrolment number**,
   **Bar Council state** (State Bar Council list) and **Year of enrolment**. They are validated on the server too
   (number format, known council, year between 1950 and the current year, no duplicate number within a state).
2. **Pending:** the new advocate is saved with `verification_status = 'PENDING'`. They can sign in and finish their profile,
   but they are **not visible to clients** and cannot be booked.
3. **Admin review:** the admin dashboard has an **Advocate verification** section listing each advocate's enrolment number,
   state, year, contact details and status (filter: Pending / Approved / Rejected). The admin can **Approve**, **Reject**
   or **Revoke** with an optional note that the advocate sees.
4. **Approved only:** once approved, the advocate appears in the *Find advocates* directory (with a "Verified advocate" badge),
   the client booking form and the AI legal guide suggestions. Booking is also blocked server-side for unapproved advocates.
5. **Rejected:** the advocate sees the admin's note on their dashboard and profile, corrects the details and resubmits
   (status returns to Pending). Approved details are locked; only an admin can change them.

Bar Council details are visible only to the advocate and to administrators, never to clients.

## Latest interface improvements
- Distinctive Advocate Connect identity with a legal-scales logo mark and responsive brand lockup.
- Reworked home page with dedicated client, advocate and administrator value sections plus a simple three-step journey.
- Admin dashboard includes platform totals, account status controls, protected admin accounts, searchable/filterable user directory, and searchable/filterable appointment overview.
- Client and advocate appointment tables include live search alongside status tabs.
- Improved dashboard spacing, mobile layouts, table readability, status colours and interaction feedback.

## Features
- **AI legal guide:** keywords such as "property dispute", "divorce", "cheque bounced" are matched to a practice area; the guide lists documents and questions, optionally adds a Gemini explanation (with or without keywords), offers tappable keyword suggestions, keeps the conversation going for follow-up questions, and suggests advocates for that area (ranked by match, rating, experience) with a Book button.
- **Client:** stats, next-appointment banner, status filter tabs, book, reschedule pending requests, cancel, read advocate notes, rate completed consultations, double-booking and past-date checks.
- **Advocate:** stats and rating, upcoming schedule, approve / reject / complete / cancel with an optional note to the client, client reviews, profile (practice areas, city, languages, fee, bio) and validated JPG/PNG/WEBP profile photo upload (2 MB maximum).
- **Directory:** search by name or case keyword, filter by practice area and city, sort by experience, rating or fee. Advocate profile photos are shared across the directory, admin verification list, client appointment history and AI advocate suggestions.
- **Everyone:** My account page (edit name/phone, change password), show-password checkbox on password fields.
- **Admin:** advocate KYC verification (approve / reject / revoke), enable / disable accounts, appointment overview.

## Structure
```text
pom.xml, nb-configuration.xml
database/advocate_connect_all_in_one.sql (single schema + verification + photo + demo-data installer)
src/main/resources/application.properties      DB + Gemini settings
src/main/java/in/advocateportal/               Config, DB, Auth, Portal, AIHelper, LegalCategories, BarCouncils, Util
src/main/webapp/                               JSPs, assets, WEB-INF/web.xml, META-INF/context.xml
```
Keywords, keyword suggestions ("hints") and practice areas can be edited in `LegalCategories.java`.

All source files use 4-space indentation (Java, JSP, HTML, CSS, JavaScript, SQL) and UTF-8 encoding.


## Database/code compatibility checks

The consolidated schema contains the columns used by the Java/JSP application:
- `users`: `id`, `name`, `email`, `phone`, `password`, `role`, `status`, `created_at`
- `advocate_profiles`: profile fields, `profile_photo`, Bar Council KYC fields, verification status/note/audit fields
- `appointments`: client/advocate IDs, date/time, topic/details, status and advocate note
- `reviews`: appointment/client/advocate IDs, rating and comment

The `Portal` servlet uses `appointments`, `advocate_profiles`, `users`, and `reviews`; `Auth` uses `users` and `advocate_profiles`; `AIHelper` reads advocate profile and review fields. The combined installer preserves these names and MySQL `ENUM` values used by the role/status checks.

If Workbench reports an import error, check that the server is MySQL 8.0+, the complete script was selected/executed, and the MySQL account has permission to create databases and tables. Do not import only a selected portion of the script.
