package in.advocateportal;

/** Small shared helpers: HTML escaping, JSON escaping and flash messages. */
public final class Util {

    private Util() {
    }

    /** Escape text before printing it in HTML (prevents XSS). */
    public static String esc(Object o) {
        if (o == null) {
            return "";
        }
        String s = String.valueOf(o);
        StringBuilder b = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&':
                    b.append("&amp;");
                    break;
                case '<':
                    b.append("&lt;");
                    break;
                case '>':
                    b.append("&gt;");
                    break;
                case '"':
                    b.append("&quot;");
                    break;
                case '\'':
                    b.append("&#39;");
                    break;
                default:
                    b.append(c);
            }
        }
        return b.toString();
    }

    /** Escape text for use inside a JSON string value (without the surrounding quotes). */
    public static String json(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    b.append("\\\"");
                    break;
                case '\\':
                    b.append("\\\\");
                    break;
                case '\n':
                    b.append("\\n");
                    break;
                case '\r':
                    b.append("\\r");
                    break;
                case '\t':
                    b.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
            }
        }
        return b.toString();
    }

    /** Message text for a short code passed as ?msg=code (codes only, never free text). */
    public static String flashText(String code) {
        if (code == null) {
            return null;
        }
        switch (code) {
            case "booked":
                return "Consultation request sent. The advocate will review it soon.";
            case "cancelled":
                return "The appointment was cancelled.";
            case "updated":
                return "Appointment status updated.";
            case "saved":
                return "Profile saved. You will now appear in AI legal guide suggestions for these practice areas.";
            case "slot":
                return "That advocate already has an approved booking at that date and time. Please choose another slot.";
            case "dup":
                return "You already have a request with this advocate at that date and time.";
            case "past":
                return "Please choose a date and time in the future.";
            case "conflict":
                return "You already approved another appointment at that date and time.";
            case "nospec":
                return "Please select at least one practice area.";
            case "rescheduled":
                return "Your request was moved to the new date and time.";
            case "reviewed":
                return "Thank you! Your review was saved.";
            case "account":
                return "Your account details were updated.";
            case "pwchanged":
                return "Your password was changed.";
            case "pwwrong":
                return "The current password is not correct.";
            case "pwshort":
                return "The new password must be 8 to 100 characters.";
            case "pwmismatch":
                return "The new password and confirmation do not match.";
            case "vsubmitted":
                return "Verification details submitted. An administrator will review them before you appear to clients.";
            case "vapproved":
                return "Advocate approved. They are now visible to clients.";
            case "vrejected":
                return "Advocate verification rejected. They are hidden from clients.";
            case "vinvalid":
                return "Please enter a valid Bar Council enrolment number, state and year of enrolment.";
            case "vdup":
                return "That Bar Council enrolment number is already registered for the selected state.";
            case "vlocked":
                return "Your verification details are approved and locked. Contact an administrator to change them.";
            case "invalid":
                return "Something was wrong with that request. Please check the details and try again.";
            default:
                return null;
        }
    }

    private static boolean isError(String code) {
        return "slot".equals(code) || "dup".equals(code) || "past".equals(code)
                || "conflict".equals(code) || "nospec".equals(code) || "invalid".equals(code)
                || "pwwrong".equals(code) || "pwshort".equals(code) || "pwmismatch".equals(code)
                || "vinvalid".equals(code) || "vdup".equals(code) || "vlocked".equals(code) || "vrejected".equals(code);
    }

    /** Ready-to-print HTML for a flash message, or an empty string. */
    public static String flashHtml(String code) {
        String t = flashText(code);
        if (t == null) {
            return "";
        }
        return "<div class=\"" + (isError(code) ? "alert" : "success") + "\">" + esc(t) + "</div>";
    }
}
