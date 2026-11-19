package in.advocateportal;

import java.time.Year;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/** State Bar Councils and validation of the advocate KYC details (enrolment number, state, year). */
public final class BarCouncils {

    public static final List<String> STATES = Collections.unmodifiableList(Arrays.asList(
            "Andhra Pradesh",
            "Assam, Nagaland, Meghalaya, Manipur, Tripura, Mizoram & Arunachal Pradesh",
            "Bihar",
            "Chhattisgarh",
            "Delhi",
            "Gujarat",
            "Himachal Pradesh",
            "Jammu & Kashmir and Ladakh",
            "Jharkhand",
            "Karnataka",
            "Kerala",
            "Madhya Pradesh",
            "Maharashtra & Goa",
            "Odisha",
            "Punjab & Haryana",
            "Rajasthan",
            "Sikkim",
            "Tamil Nadu & Puducherry",
            "Telangana",
            "Uttar Pradesh",
            "Uttarakhand",
            "West Bengal"));

    /** Letters, digits, spaces and the separators / - . used in enrolment numbers such as MAH/1234/2014. */
    private static final Pattern NUMBER = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9/\\-. ]{2,39}$");

    private static final int FIRST_YEAR = 1950;

    private BarCouncils() {
    }

    public static boolean validState(String state) {
        return state != null && STATES.contains(state);
    }

    public static boolean validNumber(String number) {
        return number != null && NUMBER.matcher(number).matches();
    }

    public static boolean validYear(int year) {
        return year >= FIRST_YEAR && year <= Year.now().getValue();
    }

    public static int maxYear() {
        return Year.now().getValue();
    }

    public static int minYear() {
        return FIRST_YEAR;
    }
}
