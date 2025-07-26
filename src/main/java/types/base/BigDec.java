package types.base;

import java.math.BigInteger;

/**
 * <p>
 * BigDec is a class that represents **arbitrary-precision decimal numbers**-
 * just like how Java's BigDecimal works, but with a slightly different internal model,
 * simpler and closer to how people manually work with scientific notation.
 * It stores the number as a mantissa and an exponent, allowing for efficient
 * arithmetic operations and comparisons.
 * </p>
 *
 * <p>
 * A value x is represented internally as: {@code value = mantissa × 10^exponent},
 * where mantissa is a BigInteger and exponent is an integer.
 * </p>
 */
public final class BigDec implements Comparable<BigDec> {

    //----- FIELDS -----
    /** The unscaled significant digits, may be negative or exceed native int ranges. */
    private final BigInteger mantissa;

    /** The exponent for decimal scaling (i.e., power of ten). */
    private final int exponent;

    /** Constant BigDec representation for decimal zero (0). */
    public static final BigDec ZERO = fromInt(0);

    /** Constant for ten (10) as BigInteger, used internally for scaling operations. */
    private static final BigInteger TEN = BigInteger.TEN;


    // ----- CONSTRUCTORS -----
    /**
     * Constructs and normalizes a BigDec from mantissa and exponent.
     * Trailing zeros in mantissa are removed, and exponent adjusted.
     *
     * @param mantissa the unscaled value
     * @param exponent the decimal exponent (power of ten)
     */
    public BigDec(BigInteger mantissa, int exponent) {
        if (mantissa.equals(BigInteger.ZERO)) {
            this.mantissa = mantissa;
            this.exponent = 0;
        } else {
            while (mantissa.mod(TEN).equals(BigInteger.ZERO)) {
                mantissa = mantissa.divide(TEN);
                exponent += 1;
            }
            this.mantissa = mantissa;
            this.exponent = exponent;
        }
    }

    // ----- ACCESS -----

    /**
     * Returns the mantissa (the significant integer digits) of this decimal.
     *
     * @return the mantissa as {@link BigInteger}
     */
    public BigInteger getMantissa() {
        return this.mantissa;
    }

    /**
     * Returns the exponent (the decimal scale) of this decimal.
     *
     * @return the exponent as {@code int}
     */
    public int getExponent() {
        return this.exponent;
    }

    // ----- FACTORY METHODS -----

    /**
     * Constructs a BigDec from a plain integer value (exponent = 0).
     *
     * @param v integer value
     * @return BigDec representation of the integer
     */
    public static BigDec fromInt(int v) {
        return new BigDec(BigInteger.valueOf(v), 0);
    }

    /**
     * Constructs a BigDec from a {@link BigInteger} integer value (exponent = 0).
     *
     * @param v integer value as BigInteger
     * @return BigDec representation of the value
     */
    public static BigDec fromBigInt(BigInteger v) {
        return new BigDec(v, 0);
    }

    /**
     * Parses a decimal number from string, supporting formats like "123.45", "1e2", "-0.003e-10".
     * The number is normalized internally (trailing zeros in mantissa removed, exponent adjusted).
     *
     * @param v string to parse
     * @return the parsed BigDec
     * @throws NumberFormatException if input is null, malformed, or cannot be parsed
     */
    public static BigDec fromString(String v) {
        if (v == null) throw new NumberFormatException();

        BigInteger integral, fraction;
        int exponent = 0;

        int len = v.length();

        int i = v.indexOf('e');
        if (i >= 0) {
            if (i + 1 == v.length()) throw new NumberFormatException();
            exponent = Integer.parseInt(v.substring(i + 1, len));
            len = i;
        }

        int fractionLen = 0;
        i = v.indexOf('.');
        if (i >= 0) {
            if (i + 1 == v.length()) throw new NumberFormatException();
            fractionLen = len - i - 1;
            String fracPart = v.substring(i + 1, i + 1 + fractionLen);
            fraction = new BigInteger(fracPart.isEmpty() ? "0" : fracPart);
            len = i;
        } else {
            fraction = BigInteger.ZERO;
        }
        String intPart = v.substring(0, len).isEmpty() ? "0" : v.substring(0, len);
        integral = new BigInteger(intPart);

        if (!fraction.equals(BigInteger.ZERO)) {
            while (fractionLen > 0) {
                integral = integral.multiply(TEN);
                exponent -= 1;
                fractionLen -= 1;
            }
        }

        if (integral.signum() == -1) {
            return new BigDec(integral.subtract(fraction), exponent);
        } else {
            return new BigDec(integral.add(fraction), exponent);
        }
    }

    // ----- COMPARISON OPERATIONS -----

    /**
     * Compares this BigDec with another BigDec.
     * Returns 0 if they are equal, -1 if this is less than that, and 1 if this is greater than that.
     *
     * @param that the BigDec to compare with
     * @return comparison result as an integer
     */
    @Override
    public int compareTo(BigDec that) {
        if (this.mantissa.equals(that.mantissa) && this.exponent == that.exponent) return 0;
        BigDec d = subtract(this, that);
        return d.isNegative() ? -1 : 1;
    }

    /**
     * Checks if this BigDec is equal to another object.
     * Two BigDecs are equal if their mantissas and exponents are the same.
     *
     * @param obj the object to compare with
     * @return true if they are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BigDec)) return false;
        BigDec that = (BigDec) obj;
        return this.mantissa.equals(that.mantissa) && this.exponent == that.exponent;
    }

    /**
     * Returns a hash code for this BigDec.
     * The hash code is computed based on the mantissa and exponent.
     *
     * @return the hash code as an integer
     */
    @Override
    public int hashCode() {
        return mantissa.hashCode() * 13 + exponent;
    }

    // ----- STRING REPRESENTATION -----

    /**
     * Returns scientific-notation string representation: mantissa followed by "e" and exponent.
     *
     * @return string in the format "12345e-3"
     */
    @Override
    public String toString() {
        return mantissa.toString() + "e" + exponent;
    }

    /**
     * Returns formatted decimal string with at most {@code maxDigits} digits after the decimal point.
     * Rounds by truncation. For values too large to fit, prints a capped value with ".0".
     *
     * @param maxDigits maximum allowed decimal digits
     * @return string representation
     */
    public String toDecimalString(int maxDigits) {
        String s = mantissa.toString();
        int digits = mantissa.signum() >= 0 ? s.length() : s.length() - 1;
        BigInteger max = BigInteger.TEN.pow(maxDigits);
        BigInteger min = max.negate();

        if (exponent >= 0) {
            if (maxDigits < digits || maxDigits - digits < exponent) {
                return String.format("%s.0", mantissa.signum() >= 0 ? max : min);
            } else {
                return String.format("%s%s.0", s, zeros(exponent));
            }
        } else {
            int exp = -exponent;
            if (exp < digits) {
                int intDigits = digits - exp;
                if (maxDigits < intDigits) {
                    return String.format("%s.0", mantissa.signum() >= 0 ? max : min);
                } else {
                    int fracDigits = Math.min(maxDigits, digits - intDigits);
                    return String.format("%s.%s", s.substring(0, intDigits),
                            s.substring(intDigits, intDigits + fracDigits));
                }
            } else {
                int fracDigits = Math.min(maxDigits, digits);
                return String.format("0.%s%s", zeros(exp - fracDigits), s.substring(0, fracDigits));
            }
        }
    }

    /**
     * Returns normalized decimal string: no scientific notation, minimal canonical form,
     * e.g., "123.45", "0.00123", etc.
     *
     * @return canonical decimal string
     */
    public String toDecimalString() {
        String m = mantissa.toString();
        int e = exponent;
        if (e >= 0) {
            return m + zeros(e) + ".0";
        } else {
            e = -e;
            int maxK = Math.min(e, m.length() - 1);
            int last = m.length() - 1;
            int k = 0;
            while (k < maxK && m.charAt(last - k) == '0') k++;
            if (k > 0) {
                m = m.substring(0, m.length() - k);
                e -= k;
            }

            if (e == 0) {
                return m;
            } else if (e < m.length()) {
                int n = m.length() - e;
                return m.substring(0, n) + "." + m.substring(n);
            } else {
                return "0." + zeros(e - m.length()) + m;
            }
        }
    }

    /**
     * Returns a string of n consecutive zeros. Used internally for formatting.
     *
     * @param n count of zeros to return
     * @return string of '0' characters
     * @throws IllegalArgumentException if n &lt; 0
     */
    public static String zeros(int n) {
        if (n < 0) throw new IllegalArgumentException();
        if (n <= 10) {
            return "0000000000".substring(0, n);
        } else {
            int d = n / 2;
            String s = zeros(d);
            return n % 2 == 0 ? s + s : s + s + "0";
        }
    }

    // ----- FLOOR & CEILING (INTEGER CONVERSION) -----

    /**
     * Returns the floor and ceiling of this value as BigInteger integers.
     * Floor rounds toward negative infinity, ceiling toward positive infinity.
     *
     * @return two-element array: [floor, ceiling], as per mathematical definition
     * @throws AssertionError if floor &gt; ceiling due to internal inconsistency
     */
    public BigInteger[] floorCeiling() {
        BigInteger n = mantissa;
        int e = exponent;
        BigInteger floor, ceiling;

        if (n.equals(BigInteger.ZERO)) {
            floor = ceiling = n;
        } else if (e >= 0) {
            for (; e > 0; e--) n = n.multiply(TEN);
            floor = ceiling = n;
        } else {
            for (; e < 0 && !n.equals(BigInteger.ZERO); e++) n = n.divide(TEN);
            if (mantissa.compareTo(BigInteger.ZERO) >= 0) {
                floor = n;
                ceiling = n.add(BigInteger.ONE);
            } else {
                ceiling = n;
                floor = n.subtract(BigInteger.ONE);
            }
        }
        if (floor.compareTo(ceiling) > 0)
            throw new AssertionError("Invariant was not maintained");
        return new BigInteger[] { floor, ceiling };
    }

    // ----- BASIC ARITHMETIC OPERATIONS -----

    /**
     * Returns the absolute value of this BigDec.
     *
     * @return absolute (always non-negative) value
     */
    public BigDec abs() {
        return new BigDec(mantissa.abs(), exponent);
    }

    /**
     * Returns the negation of this BigDec.
     *
     * @return the value multiplied by -1
     */
    public BigDec negate() {
        return new BigDec(mantissa.negate(), exponent);
    }

    /**
     * Returns the negation of the given BigDec.
     * This is a static utility method for convenience.
     *
     * @param x the BigDec to negate
     * @return a new BigDec that is the negation of x
     */
    public static BigDec negate(BigDec x) {
        return x.negate();
    }

    /**
     * Adds two BigDec values, canonicalizing result. Exponents are aligned as needed.
     *
     * @param x first operand
     * @param y second operand
     * @return sum x+y as BigDec
     */
    public static BigDec add(BigDec x, BigDec y) {
        BigInteger m1 = x.mantissa;
        int e1 = x.exponent;
        BigInteger m2 = y.mantissa;
        int e2 = y.exponent;
        // Ensure e1 <= e2
        if (e2 < e1) {
            BigInteger tmpm = m1; m1 = m2; m2 = tmpm;
            int tmpe = e1; e1 = e2; e2 = tmpe;
        }
        while (e2 > e1) {
            m2 = m2.multiply(TEN);
            e2--;
        }
        return new BigDec(m1.add(m2), e1);
    }

    /**
     * Subtracts one BigDec from another, canonicalizing result. Equivalent to add(x, negate(y)).
     *
     * @param x left operand
     * @param y right operand
     * @return result x-y as BigDec
     */
    public static BigDec subtract(BigDec x, BigDec y) {
        return add(x, y.negate());
    }

    /**
     * Multiplies two BigDec values.
     * The result mantissa is x.mantissa * y.mantissa, exponent is x.exponent + y.exponent.
     *
     * @param x first operand
     * @param y second operand
     * @return product x*y as BigDec
     */
    public static BigDec multiply(BigDec x, BigDec y) {
        return new BigDec(x.mantissa.multiply(y.mantissa), x.exponent + y.exponent);
    }

    // ----- PROPERTY QUERIES -----

    /**
     * Checks whether the number is greater than zero.
     *
     * @return true if positive
     */
    public boolean isPositive() {
        return mantissa.compareTo(BigInteger.ZERO) > 0;
    }

    /**
     * Checks whether the number is less than zero.
     *
     * @return true if negative
     */
    public boolean isNegative() {
        return mantissa.compareTo(BigInteger.ZERO) < 0;
    }

    /**
     * Checks whether the number equals zero.
     *
     * @return true if mantissa is zero
     */
    public boolean isZero() {
        return mantissa.equals(BigInteger.ZERO);
    }

    // ----- CONVENIENCE STATIC COMPARISON METHODS (No operator overloading in Java) -----

    /**
     * Returns true if {@code this} numerically equals {@code other}.
     */
    public boolean eq(BigDec other) { return this.compareTo(other) == 0; }

    /**
     * Returns true if {@code this} is not equal to {@code other}.
     */
    public boolean ne(BigDec other) { return this.compareTo(other) != 0; }

    /**
     * Returns true if {@code this} is less than {@code other}.
     */
    public boolean lt(BigDec other) { return this.compareTo(other) < 0; }

    /**
     * Returns true if {@code this} is greater than {@code other}.
     */
    public boolean gt(BigDec other) { return this.compareTo(other) > 0; }

    /**
     * Returns true if {@code this} is less than or equal to {@code other}.
     */
    public boolean le(BigDec other) { return this.compareTo(other) <= 0; }

    /**
     * Returns true if {@code this} is greater than or equal to {@code other}.
     */
    public boolean ge(BigDec other) { return this.compareTo(other) >= 0; }
}
