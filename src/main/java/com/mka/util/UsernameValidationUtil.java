package com.mka.util;

import com.mka.exception.ValidationException;
import com.mka.repository.ProfileRepository;

import java.util.*;

public class UsernameValidationUtil {

    // Comprehensive collection of Indian & Global real human first/last names (blocked to guarantee total anonymity)
    private static final Set<String> REAL_HUMAN_NAMES = new HashSet<>(Arrays.asList(
            "pratik", "prateek", "raaju", "raju", "prajwal", "rahul", "priya", "amit", "suresh", "anita", "sunil", "vikram",
            "ajay", "sanjay", "manish", "pooja", "neha", "deepak", "rohit", "ankit", "sumit", "karan",
            "arjun", "varun", "aditya", "abhishek", "ravi", "vijay", "ashok", "alok", "divya", "kavita",
            "sunita", "rekha", "rashi", "neeraj", "sachin", "dhoni", "virat", "ramesh", "dinesh", "naresh",
            "mahesh", "gautam", "patel", "kumar", "singh", "sharma", "verma", "gupta", "yadav", "chawla",
            "reddy", "nair", "deshmukh", "prashant", "pratham", "praveen", "pravin", "pramod", "prabhat", "prakash",
            "pradeep", "preeti", "priyanka", "prerna", "pratibha", "john", "michael", "david", "james", "robert", "william", "mary",
            "patricia", "linda", "barbara", "elizabeth", "jennifer", "maria", "susan", "margaret", "lisa",
            "nancy", "karen", "betty", "helen", "sandra", "donna", "carol", "ruth", "sharon", "michelle",
            "laura", "sarah", "kimberly", "deborah", "jessica", "shirley", "cynthia", "angela", "melissa",
            "brenda", "amy", "anna", "rebecca", "virginia", "kathleen", "pamela", "martha", "debra", "amanda",
            "stephanie", "carolyn", "christine", "marie", "janet", "catherine", "frances", "ann", "joyce",
            "diane", "alice", "julie", "heather", "teresa", "doris", "gloria", "evelyn", "jean", "cheryl",
            "mildred", "katherine", "joan", "ashley", "judith", "rose", "janice", "kelly", "nicole", "judy",
            "christina", "kathy", "theresa", "beverly", "denise", "tammy", "irene", "jane", "lori", "rachel",
            "marilyn", "andrea", "kathryn", "louise", "sara", "anne", "jacqueline", "wanda", "bonnie", "julia",
            "ruby", "lois", "tina", "phyllis", "norma", "paula", "diana", "annie", "lillian", "emily", "robin"
    ));

    // Abusive / Harmful / Profane words filter
    private static final Set<String> ABUSIVE_WORDS = new HashSet<>(Arrays.asList(
            "fuck", "shit", "bitch", "asshole", "bastard", "chutiya", "madarchod", "bhenchod",
            "gand", "gaand", "lauda", "lode", "harami", "kamina", "terrorist", "nazi", "hitler",
            "murder", "kill", "rape", "slut", "whore", "cunt", "dick", "pussy"
    ));

    public static void validateUsername(String rawUsername) {
        validateUsername(rawUsername, null);
    }

    /**
     * Validates that username does not contain real human names, user's own full name tokens, or abusive words.
     */
    public static void validateUsername(String rawUsername, String userFullName) {
        if (rawUsername == null || rawUsername.isBlank()) {
            throw new ValidationException("Username handle cannot be empty.");
        }

        String clean = rawUsername.trim().toLowerCase().replaceAll("^@", "");

        if (clean.length() < 3 || clean.length() > 30) {
            throw new ValidationException("Username handle must be between 3 and 30 characters.");
        }

        // 1. Abusive words check
        for (String abusive : ABUSIVE_WORDS) {
            if (clean.contains(abusive)) {
                throw new ValidationException("Username contains inappropriate or restricted words. Please choose a clean, respectful handle.");
            }
        }

        // 2. User's Own Full Name Check (e.g. if registered name is "Pratik Sharma", block handles containing "pratik" or "sharma")
        if (userFullName != null && !userFullName.isBlank()) {
            String[] tokens = userFullName.trim().toLowerCase().split("\\s+");
            for (String token : tokens) {
                if (token.length() >= 3 && clean.contains(token)) {
                    throw new ValidationException("Username cannot contain your real name ('" + token + "') to maintain absolute anonymity. Try fictional handles like 'captainamerica' or 'cyberninja'.");
                }
            }
        }

        // 3. Real human names check (substring containment)
        for (String humanName : REAL_HUMAN_NAMES) {
            if (clean.contains(humanName)) {
                throw new ValidationException("Real human names like '" + humanName + "' are not allowed to maintain anonymity. Try fictional or creative handles like 'captainamerica' or 'cyberninja'.");
            }
        }
    }

    /**
     * Generates numbered available username suggestions when base username is taken
     */
    public static List<String> generateAvailableSuggestions(String baseUsername, ProfileRepository profileRepository) {
        String cleanBase = baseUsername.trim().replaceAll("^@", "");
        cleanBase = cleanBase.replaceAll("\\d+$", "");
        if (cleanBase.length() < 3) cleanBase = "anonymous";

        List<String> suggestions = new ArrayList<>();
        Random random = new Random();
        int attempts = 0;

        while (suggestions.size() < 4 && attempts < 100) {
            attempts++;
            int num = 10 + random.nextInt(90);
            String candidate = cleanBase + num;
            if (!profileRepository.existsByUsername(candidate) && !suggestions.contains(candidate)) {
                suggestions.add(candidate);
            }
        }

        return suggestions;
    }
}
