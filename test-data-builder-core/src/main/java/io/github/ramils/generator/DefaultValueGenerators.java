package io.github.ramils.generator;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import io.github.ramils.metadata.PropertyMetadata;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default value generators for common Java types.
 */
public final class DefaultValueGenerators {

    private static final Random RANDOM = new Random();

    // Singleton instances of all generators
    private static final EnumGenerator ENUM_GENERATOR = new EnumGenerator();
    private static final EmailGenerator EMAIL_GENERATOR = new EmailGenerator();
    private static final PatternGenerator PATTERN_GENERATOR = new PatternGenerator();
    private static final StringGenerator STRING_GENERATOR = new StringGenerator();
    private static final NumberGenerator NUMBER_GENERATOR = new NumberGenerator();
    private static final BooleanGenerator BOOLEAN_GENERATOR = new BooleanGenerator();
    private static final TemporalGenerator TEMPORAL_GENERATOR = new TemporalGenerator();
    private static final UuidGenerator UUID_GENERATOR = new UuidGenerator();
    private static final ByteArrayGenerator BYTE_ARRAY_GENERATOR = new ByteArrayGenerator();
    private static final CollectionGenerator COLLECTION_GENERATOR = new CollectionGenerator();

    // Cached immutable list of all generators in priority order
    private static final List<ValueGenerator> ALL_GENERATORS = List.of(
            ENUM_GENERATOR,
            EMAIL_GENERATOR,
            PATTERN_GENERATOR,
            STRING_GENERATOR,
            NUMBER_GENERATOR,
            BOOLEAN_GENERATOR,
            TEMPORAL_GENERATOR,
            UUID_GENERATOR,
            BYTE_ARRAY_GENERATOR,
            COLLECTION_GENERATOR
    );

    private DefaultValueGenerators() {
    }

    /**
     * Returns all default generators in priority order.
     * Returns a cached immutable list - no new objects are created on each call.
     */
    public static List<ValueGenerator> getAll() {
        return ALL_GENERATORS;
    }

    /**
     * Generates enum values (first value by default).
     */
    public static class EnumGenerator implements ValueGenerator {
        @Override
        public boolean supports(PropertyMetadata property) {
            return property.getType().isEnum();
        }

        @Override
        public Object generate(PropertyMetadata property, GeneratorContext context) {
            Object[] constants = property.getType().getEnumConstants();
            if (constants.length == 0) {
                return null;
            }
            // Return first enum value (consistent with Grails build-test-data)
            return constants[0];
        }

        @Override
        public int getPriority() {
            return 100;
        }
    }

    /**
     * Generates email addresses for @Email annotated fields.
     */
    public static class EmailGenerator implements ValueGenerator {
        @Override
        public boolean supports(PropertyMetadata property) {
            return property.isEmail() ||
                   (property.getType() == String.class &&
                    property.getName().toLowerCase().contains("email"));
        }

        @Override
        public Object generate(PropertyMetadata property, GeneratorContext context) {
            String fieldKey = property.getDeclaringClass().getName() + "." + property.getName();
            int counter = property.isUnique()
                    ? context.nextUniqueCounter(fieldKey) : context.getSequenceIndex();
            String suffix = counter == 0 ? "" : String.valueOf(counter);
            return property.getName() + suffix + "@test.com";
        }

        @Override
        public int getPriority() {
            return 90;
        }
    }

    /**
     * Generates strings matching @Pattern regex.
     * Uses cached Automaton instances for better performance.
     */
    public static class PatternGenerator implements ValueGenerator {

        // Cache for compiled Automaton instances by regex pattern
        private static final Map<String, Automaton> AUTOMATON_CACHE = new ConcurrentHashMap<>();

        @Override
        public boolean supports(PropertyMetadata property) {
            return property.getType() == String.class && property.getPattern() != null;
        }

        @Override
        public Object generate(PropertyMetadata property, GeneratorContext context) {
            try {
                return generateFromRegex(property.getPattern());
            } catch (Exception e) {
                // Fallback to simple string if regex generation fails
                return property.getName();
            }
        }

        private String generateFromRegex(String regex) {
            // Use cached Automaton or create and cache a new one
            Automaton automaton = AUTOMATON_CACHE.computeIfAbsent(regex, r -> {
                RegExp regExp = new RegExp(r);
                return regExp.toAutomaton();
            });
            return generateStringFromAutomaton(automaton);
        }

        private String generateStringFromAutomaton(Automaton automaton) {
            StringBuilder sb = new StringBuilder();
            State state = automaton.getInitialState();

            boolean shouldContinue = !state.isAccept()
                    || (state.isAccept() && !state.getTransitions().isEmpty() && RANDOM.nextBoolean());
            while (shouldContinue) {
                List<Transition> transitions = new ArrayList<>(state.getTransitions());
                if (transitions.isEmpty()) {
                    break;
                }

                Transition transition = transitions.get(RANDOM.nextInt(transitions.size()));
                char c = (char) (transition.getMin()
                        + RANDOM.nextInt(transition.getMax() - transition.getMin() + 1));
                sb.append(c);
                state = transition.getDest();

                // Prevent infinite loops
                if (sb.length() > 100) {
                    break;
                }

                shouldContinue = !state.isAccept()
                        || (state.isAccept() && !state.getTransitions().isEmpty() && RANDOM.nextBoolean());
            }

            return sb.toString();
        }

        /**
         * Clears the automaton cache. Useful for testing or memory management.
         */
        public static void clearCache() {
            AUTOMATON_CACHE.clear();
        }

        @Override
        public int getPriority() {
            return 80;
        }
    }

    /**
     * Generates string values respecting size constraints.
     */
    public static class StringGenerator implements ValueGenerator {
        @Override
        public boolean supports(PropertyMetadata property) {
            return property.getType() == String.class;
        }

        @Override
        public Object generate(PropertyMetadata property, GeneratorContext context) {
            String baseValue = property.getName();
            String fieldKey = property.getDeclaringClass().getName() + "." + property.getName();

            // Add counter for unique fields
            if (property.isUnique()) {
                int counter = context.nextUniqueCounter(fieldKey);
                if (counter > 0) {
                    baseValue = baseValue + counter;
                }
            }

            // Respect size constraints
            Integer minLength = property.getMinLength();
            Integer maxLength = property.getMaxLength();

            if (maxLength != null && baseValue.length() > maxLength) {
                baseValue = baseValue.substring(0, maxLength);
            }

            if (minLength != null && baseValue.length() < minLength) {
                // Pad with repeated characters
                StringBuilder sb = new StringBuilder(baseValue);
                while (sb.length() < minLength) {
                    sb.append('x');
                }
                baseValue = sb.toString();
            }

            return baseValue;
        }

        @Override
        public int getPriority() {
            return 10;
        }
    }

    /**
     * Generates numeric values respecting min/max constraints.
     */
    public static class NumberGenerator implements ValueGenerator {
        @Override
        public boolean supports(PropertyMetadata property) {
            Class<?> type = property.getType();
            return Number.class.isAssignableFrom(type)
                   || type == int.class || type == long.class || type == short.class
                   || type == byte.class || type == float.class || type == double.class;
        }

        @Override
        public Object generate(PropertyMetadata property, GeneratorContext context) {
            Class<?> type = property.getType();
            Number min = property.getMinValue();
            Number max = property.getMaxValue();

            // Determine default value respecting constraints
            long value = 0;
            if (min != null) {
                value = Math.max(value, min.longValue());
            }
            if (max != null) {
                value = Math.min(value, max.longValue());
            }

            // Add uniqueness if needed
            if (property.isUnique()) {
                String fieldKey = property.getDeclaringClass().getName() + "." + property.getName();
                value = value + context.nextUniqueCounter(fieldKey);
            }

            return convertToType(value, type);
        }

        private Object convertToType(long value, Class<?> type) {
            if (type == Integer.class || type == int.class) {
                return (int) value;
            } else if (type == Long.class || type == long.class) {
                return value;
            } else if (type == Short.class || type == short.class) {
                return (short) value;
            } else if (type == Byte.class || type == byte.class) {
                return (byte) value;
            } else if (type == Float.class || type == float.class) {
                return (float) value;
            } else if (type == Double.class || type == double.class) {
                return (double) value;
            } else if (type == BigDecimal.class) {
                return BigDecimal.valueOf(value);
            } else if (type == BigInteger.class) {
                return BigInteger.valueOf(value);
            }
            return value;
        }

        @Override
        public int getPriority() {
            return 10;
        }
    }

    /**
     * Generates boolean values.
     */
    public static class BooleanGenerator implements ValueGenerator {
        @Override
        public boolean supports(PropertyMetadata property) {
            Class<?> type = property.getType();
            return type == Boolean.class || type == boolean.class;
        }

        @Override
        public Object generate(PropertyMetadata property, GeneratorContext context) {
            return false; // Default to false (consistent with Grails)
        }

        @Override
        public int getPriority() {
            return 10;
        }
    }

    /**
     * Generates temporal values (dates, times).
     */
    public static class TemporalGenerator implements ValueGenerator {
        @Override
        public boolean supports(PropertyMetadata property) {
            Class<?> type = property.getType();
            return type == LocalDate.class
                   || type == LocalDateTime.class
                   || type == LocalTime.class
                   || type == Instant.class
                   || type == ZonedDateTime.class
                   || type == OffsetDateTime.class
                   || type == OffsetTime.class
                   || type == Date.class
                   || type == java.sql.Date.class
                   || type == java.sql.Time.class
                   || type == java.sql.Timestamp.class
                   || type == Calendar.class;
        }

        @Override
        public Object generate(PropertyMetadata property, GeneratorContext context) {
            Class<?> type = property.getType();

            if (type == LocalDate.class) {
                return LocalDate.now();
            } else if (type == LocalDateTime.class) {
                return LocalDateTime.now();
            } else if (type == LocalTime.class) {
                return LocalTime.now();
            } else if (type == Instant.class) {
                return Instant.now();
            } else if (type == ZonedDateTime.class) {
                return ZonedDateTime.now();
            } else if (type == OffsetDateTime.class) {
                return OffsetDateTime.now();
            } else if (type == OffsetTime.class) {
                return OffsetTime.now();
            } else if (type == Date.class) {
                return new Date();
            } else if (type == java.sql.Date.class) {
                return new java.sql.Date(System.currentTimeMillis());
            } else if (type == java.sql.Time.class) {
                return new java.sql.Time(System.currentTimeMillis());
            } else if (type == java.sql.Timestamp.class) {
                return new java.sql.Timestamp(System.currentTimeMillis());
            } else if (type == Calendar.class) {
                return new GregorianCalendar();
            }

            return null;
        }

        @Override
        public int getPriority() {
            return 10;
        }
    }

    /**
     * Generates UUIDs.
     */
    public static class UuidGenerator implements ValueGenerator {
        @Override
        public boolean supports(PropertyMetadata property) {
            return property.getType() == UUID.class;
        }

        @Override
        public Object generate(PropertyMetadata property, GeneratorContext context) {
            return UUID.randomUUID();
        }

        @Override
        public int getPriority() {
            return 10;
        }
    }

    /**
     * Generates byte arrays.
     */
    public static class ByteArrayGenerator implements ValueGenerator {
        @Override
        public boolean supports(PropertyMetadata property) {
            return property.getType() == byte[].class || property.getType() == Byte[].class;
        }

        @Override
        public Object generate(PropertyMetadata property, GeneratorContext context) {
            // Return a minimal valid byte array (similar to Grails - a small GIF)
            return new byte[]{71, 73, 70, 56, 57, 97, 1, 0, 1, 0};
        }

        @Override
        public int getPriority() {
            return 10;
        }
    }

    /**
     * Generates empty collections.
     */
    public static class CollectionGenerator implements ValueGenerator {
        @Override
        public boolean supports(PropertyMetadata property) {
            return Collection.class.isAssignableFrom(property.getType())
                   || Map.class.isAssignableFrom(property.getType());
        }

        @Override
        public Object generate(PropertyMetadata property, GeneratorContext context) {
            Class<?> type = property.getType();

            if (List.class.isAssignableFrom(type)) {
                return new ArrayList<>();
            }
            if (Set.class.isAssignableFrom(type)) {
                return new HashSet<>();
            }
            if (Map.class.isAssignableFrom(type)) {
                return new HashMap<>();
            }
            if (Collection.class.isAssignableFrom(type)) {
                return new ArrayList<>();
            }

            return null;
        }

        @Override
        public int getPriority() {
            return 5;
        }
    }
}
