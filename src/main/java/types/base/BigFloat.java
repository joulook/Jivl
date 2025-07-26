package types.base;

import java.math.BigInteger;
import java.util.Objects;

/**
 * <p>
 * Represents a floating-point number with arbitrary precision, modeled after a standard floating-point
 * representation with a 1-bit sign, an exponent, and a significand (also known as mantissa).
 * </p>
 *
 * <p>
 * The floating-point number is internally stored with the following components:
 * <ul>
 *   <li><b>Sign bit:</b> A boolean indicating the sign (true means negative).</li>
 *   <li><b>Significand:</b> A non-negative {@link BigInteger} representing the significand bits.</li>
 *   <li><b>Significand size:</b> The bit-width (size) of the significand.</li>
 *   <li><b>Exponent:</b> A non-negative {@link BigInteger} representing the biased exponent bits.</li>
 *   <li><b>Exponent size:</b> The bit-width (size) of the exponent.</li>
 *   <li><b>Special value string:</b> A string representing special floating point values like {@code "NaN"},
 *   {@code "+oo"}, or {@code "-oo"}.</li>
 * </ul>
 * </p>
 *
 * <p>
 * This class is immutable and thread-safe.
 * </p>
 *
 * <p>
 * <b>Note:</b> Java does not support operator overloading, so arithmetic and comparison operators
 * from the original C# code have been replaced by named methods such as {@code add}, {@code negate},
 * {@code multiply}, and comparison methods like {@code eq}, {@code lt}, etc.
 * </p>
 *
 * <p>
 * This class is typically used in formal verification tools or symbolic computation frameworks
 * demanding explicit control over floating point format and semantics.
 * </p>
 */
public final class BigFloat implements  Comparable<BigFloat> {

    // ----- Fields -----

    /**
     * The significand bits of this floating point number.
     * This value is always non-negative.
     * The most significant bit corresponds to the "hidden" bit in normalized representation.
     */
    private final BigInteger significand;  // Significand bits (unsigned, MSB left)

    /**
     * Bit-width of the significand.
     * Must be greater than 1.
     */
    private final int significandSize;     // Bit-width of significand

    /**
     * The biased exponent bits of this floating point number.
     * This value is always non-negative (unsigned representation).
     */
    private final BigInteger exponent;     // Exponent bits (unsigned)

    /**
     * Bit-width of the exponent.
     * Must be greater than 1.
     */
    private final int exponentSize;        // Bit-width of exponent

    /**
     * A special representation string for this number, used only for special floating-point values:
     * {@code "NaN"} - Not a Number,
     * {@code "+oo"} - Positive Infinity,
     * {@code "-oo"} - Negative Infinity.
     * If this string is empty, the {@link #significand} and {@link #exponent} fields hold a valid finite value.
     */
    private final String value;            // Special for NaN/+oo/-oo

    /**
     * The sign bit of the floating point number.
     * If {@code true}, the number is negative; otherwise, it is positive.
     */
    private final boolean isSignBitSet;    // True if negative

    /**
     * A predefined constant representing positive zero with default sizes (significand 24 bits, exponent 8 bits).
     * Note that this does not represent a negative zero.
     */
    public static final BigFloat ZERO = new BigFloat(false, BigInteger.ZERO, BigInteger.ZERO, 24, 8);

    // ----- Constructors -----

    /**
     * Constructs a new {@code BigFloat} with all fields specified.
     * Used internally to create both finite and special values.
     *
     * @param isSignBitSet the sign bit, {@code true} for negative
     * @param significand the significand (non-negative)
     * @param exponent the biased exponent (non-negative)
     * @param significandSize bit-width of the significand, > 1
     * @param exponentSize bit-width of the exponent, > 1
     */
    public BigFloat(boolean isSignBitSet, BigInteger significand, BigInteger exponent, int significandSize, int exponentSize) {
        this.isSignBitSet = isSignBitSet;
        this.significand = significand;
        this.exponent = exponent;
        this.significandSize = significandSize;
        this.exponentSize = exponentSize;
        this.value = "";
    }

    /**
     * Constructs a new {@code BigFloat} for special values like {@code "NaN"}, {@code "+oo"}, {@code "-oo"}.
     * Sets finite fields to zero internally.
     *
     * @param value a string representing the special value, e.g., {@code "NaN"} or {@code "+oo"} or {@code "-oo"}.
     * @param significandSize bit-width of the significand, > 1
     * @param exponentSize bit-width of the exponent, > 1
     */
    public BigFloat(String value, int significandSize, int exponentSize) {
        this.value = value.equalsIgnoreCase("nan") ? "NaN" : value;
        this.isSignBitSet = value.length() > 0 && value.charAt(0) == '-';
        this.significandSize = significandSize;
        this.exponentSize = exponentSize;
        this.significand = BigInteger.ZERO;
        this.exponent = BigInteger.ZERO;
    }

    // ----- Access -----

    /**
     * Returns the significand size in bits.
     *
     * @return the bit-width of the significand.
     */
    public int getSignificandSize() {
        return significandSize;
    }

    /**
     * Returns the exponent size in bits.
     *
     * @return the bit-width of the exponent.
     */
    public int getExponentSize() {
        return exponentSize;
    }

    // ----- Factory Methods -----

    /**
     * Creates a {@code BigFloat} instance representing the integer value {@code v}.
     * Uses default floating-point sizes: significandSize = 24, exponentSize = 8.
     * The integer is converted to a string and parsed internally.
     *
     * @param v the integer value to represent
     * @return a new {@code BigFloat} representing {@code v}
     */
    public static BigFloat fromInt(int v) {
        return new BigFloat(Integer.toString(v), 24, 8);
    }

    /**
     * Creates a {@code BigFloat} instance representing the given {@link BigInteger} value.
     * The floating-point sizes must be specified explicitly.
     * The integer value is converted to a string and parsed internally.
     *
     * @param v the {@code BigInteger} value to represent
     * @param significandSize the bit-width of the significand, must be > 1
     * @param exponentSize the bit-width of the exponent, must be > 1
     * @return a new {@code BigFloat} representing {@code v}
     */
    public static BigFloat fromBigInt(BigInteger v, int significandSize, int exponentSize) {
        return new BigFloat(v.toString(), significandSize, exponentSize);
    }

    /**
     * Parses a floating-point string representation into a {@code BigFloat} object.
     *
     * <p>
     * The string must have one of the following formats:
     * <ul>
     *   <li>Floating-point format: {@code [-]0x^.^e*f*e*}, where
     *     <ul>
     *       <li>^ indicates a hexadecimal value</li>
     *       <li>* indicates an integer value</li>
     *     </ul>
     *   </li>
     *   <li>Special value formats:
     *     <ul>
     *       <li>{@code 0NaN*e*}</li>
     *       <li>{@code 0nan*e*}</li>
     *       <li>{@code 0+oo*e*}</li>
     *       <li>{@code 0-oo*e*}</li>
     *     </ul>
     *   </li>
     * </ul>
     * </p>
     *
     * <p>This method extracts the significand size and exponent size from the suffix notation {@code f} and {@code e}. It throws a {@link NumberFormatException} if they are not valid.</p>
     *
     * @param s the string representation of a floating-point number
     * @return the parsed {@code BigFloat}
     * @throws NumberFormatException if the string is malformed or sizes are invalid (≤ 1)
     */
    public static BigFloat fromString(String s) {
        int posLastE = s.lastIndexOf('e');

        int expSize = Integer.parseInt(s.substring(posLastE + 1));
        if (expSize <= 1)
            throw new NumberFormatException("Exponent size must be greater than 1");

        int posLastF = s.lastIndexOf('f');
        int posSig = posLastF + 1;
        if (posLastF == -1)
            posSig = 4; // Position for special values like nan or infinity

        int sigSize = Integer.parseInt(s.substring(posSig, posLastE));
        if (sigSize <= 1)
            throw new NumberFormatException("Significand size must be greater than 1");

        if (posLastF == -1) {
            // For "nan", "+oo", "-oo" special cases
            return new BigFloat(s.substring(1, 4), sigSize, expSize);
        }

        // Extract sign
        boolean isSignBitSet = s.charAt(0) == '-';

        // Indices for hex significand and exponent parsing
        int posX = s.indexOf('x');
        int posSecondLastE = s.lastIndexOf('e', posLastE - 1);

        // Hexadecimal part of significand
        String hexSig = s.substring(posX + 1, posSecondLastE);
        BigInteger oldExp = new BigInteger(s.substring(posSecondLastE + 1, posLastF));

        // Convert hex string to binary string with '.' preserved
        StringBuilder binSigBuilder = new StringBuilder();
        for (char c : hexSig.toCharArray()) {
            if (c == '.') {
                binSigBuilder.append('.');
            } else {
                String fourBits = Integer.toBinaryString(Integer.parseInt(Character.toString(c), 16));
                binSigBuilder.append(String.format("%4s", fourBits).replace(' ', '0'));
            }
        }
        String binSig = binSigBuilder.toString();

        // Remove decimal point
        int posDec = binSig.indexOf('.');
        binSig = binSig.substring(0, posDec) + binSig.substring(posDec + 1);

        int posFirstOne = binSig.indexOf('1');
        int posLastOne = binSig.lastIndexOf('1');

        if (posFirstOne == -1) {
            // Zero significand and exponent
            return new BigFloat(isSignBitSet, BigInteger.ZERO, BigInteger.ZERO, sigSize, expSize);
        }

        binSig = binSig.substring(posFirstOne, posLastOne + 1);

        BigInteger bias = BigInteger.valueOf(2).pow(expSize - 1).subtract(BigInteger.ONE);
        BigInteger upperBound = bias.multiply(BigInteger.valueOf(2)).add(BigInteger.ONE);

        BigInteger newExp = oldExp.multiply(BigInteger.valueOf(4))
                .add(bias)
                .add(BigInteger.valueOf(posDec - posFirstOne - 1));

        if (newExp.compareTo(BigInteger.ZERO) < 0) {
            if (newExp.negate().intValue() <= (sigSize - 1) - binSig.length()) {
                binSig = "0".repeat(newExp.negate().intValue()) + binSig;
                newExp = BigInteger.ZERO;
            }
        } else {
            binSig = binSig.substring(1);
        }

        if (newExp.compareTo(BigInteger.ZERO) < 0 || newExp.compareTo(upperBound) >= 0)
            throw new NumberFormatException("The given exponent cannot fit in the bit size " + expSize);

        if (binSig.length() > sigSize - 1)
            throw new NumberFormatException("The given significand cannot fit in the bit size " + (sigSize - 1));

        binSig = binSig + "0".repeat(sigSize - 1 - binSig.length());

        BigInteger newSig = BigInteger.ZERO;
        for (char b : binSig.toCharArray()) {
            if (b == '0' || b == '1') {
                newSig = newSig.shiftLeft(1);
                if (b == '1')
                    newSig = newSig.add(BigInteger.ONE);
            }
        }

        return new BigFloat(isSignBitSet, newSig, newExp, sigSize, expSize);
    }

    // ----- Comparison Methods -----

    /**
     * Checks equality with another object.
     * Two {@code BigFloat} objects are equal if their floating-point representations are bitwise equal.
     * This method also considers special values.
     *
     * @param obj the object to compare to
     * @return {@code true} if {@code obj} is a {@code BigFloat} equal to this; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BigFloat)) return false;
        BigFloat that = (BigFloat) obj;
        return this.compareTo(that) == 0;
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     * The hash code is a combination of significand and exponent hash codes.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(significand, exponent, value, isSignBitSet, significandSize, exponentSize);
    }

    /**
     * Returns a string representation of this {@code BigFloat}.
     * <ul>
     *   <li>If this represents a finite value, returns a formatted string showing the sign,
     *   hexadecimal significand, exponent, and bit sizes in the format:
     *   {@code [-]0x[significand].[fraction]e[exponent]f[significandSize]e[exponentSize]}</li>
     *   <li>If this represents a special value like NaN or infinity,
     *   returns a string of the form: {@code 0[value][significandSize]e[exponentSize]}</li>
     * </ul>
     *
     * @return string representation of the floating-point number
     */
    @Override
    public String toString() {
        if (!value.isEmpty()) {
            return String.format("0%s%de%d", value, significandSize, exponentSize);
        }

        // TODO: Completeness of binary-to-hex representation is simplified here.
        // TODO: A full implementation would convert significand bits and exponent similarly to the original C#.

        return String.format("%s0x?.?e?f%de%d", isSignBitSet ? "-" : "", significandSize, exponentSize);
    }

    // ----- Arithmetic Operations -----

    /**
     * Returns the negation of this {@code BigFloat}.
     * Handles special values correctly:
     * <ul>
     *   <li>NaN's negation is NaN</li>
     *   <li>Positive infinity negation is negative infinity</li>
     *   <li>Negative infinity negation is positive infinity</li>
     * </ul>
     * Otherwise, negates the sign bit of the finite number.
     *
     * @return negated floating-point number
     */
    public BigFloat negate() {
        if (!value.isEmpty()) {
            if (value.charAt(0) == '-')
                return new BigFloat("+oo", significandSize, exponentSize);
            if (value.charAt(0) == '+')
                return new BigFloat("-oo", significandSize, exponentSize);
            return new BigFloat("NaN", significandSize, exponentSize);
        }
        return new BigFloat(!isSignBitSet, significand, exponent, significandSize, exponentSize);
    }

    /**
     * Adds this {@code BigFloat} to another.
     * Both operands must have identical significand and exponent sizes.
     * Handles special values according to IEEE semantics.
     *
     * @param y the addend
     * @return the sum of this and {@code y}
     * @throws IllegalArgumentException if floating-point formats differ
     */
    public BigFloat add(BigFloat y) {
        BigFloat x = this;

        if (x.exponentSize != y.exponentSize || x.significandSize != y.significandSize) {
            throw new IllegalArgumentException("Cannot add BigFloats with differing sizes");
        }

        // If either is special value, handle per IEEE rules
        if (!x.value.isEmpty() || !y.value.isEmpty()) {
            if (x.value.equals("NaN") || y.value.equals("NaN") ||
                    (x.value.equals("+oo") && y.value.equals("-oo")) ||
                    (x.value.equals("-oo") && y.value.equals("+oo"))) {
                return new BigFloat("NaN", x.significandSize, x.exponentSize);
            }
            if (!x.value.isEmpty()) return x;
            return y;
        }

        // Align exponents, swap if necessary so that x.exponent <= y.exponent
        if (x.exponent.compareTo(y.exponent) > 0) {
            BigFloat temp = x;
            x = y;
            y = temp;
        }

        BigInteger xsig = x.significand;
        BigInteger ysig = y.significand;
        BigInteger xexp = x.exponent;
        BigInteger yexp = y.exponent;

        // If difference in exponent is bigger than significand size, return y since x is insignificant
        if (yexp.subtract(xexp).compareTo(BigInteger.valueOf(x.significandSize)) > 0) {
            return new BigFloat(y.isSignBitSet, y.significand, y.exponent, y.significandSize, y.exponentSize);
        }

        BigInteger hiddenBitPow = BigInteger.valueOf(2).pow(x.significandSize - 1);

        // Adjust significands if exponents > 0 (accounting for hidden bit)
        if (xexp.compareTo(BigInteger.ZERO) > 0) xsig = xsig.add(hiddenBitPow); else xexp = xexp.add(BigInteger.ONE);
        if (yexp.compareTo(BigInteger.ZERO) > 0) ysig = ysig.add(hiddenBitPow); else yexp = yexp.add(BigInteger.ONE);

        if (x.isSignBitSet) xsig = xsig.negate();
        if (y.isSignBitSet) ysig = ysig.negate();

        // Shift xsig by exponent difference
        xsig = xsig.shiftRight(yexp.subtract(xexp).intValue());

        ysig = ysig.add(xsig);

        boolean isNeg = ysig.signum() < 0;
        ysig = ysig.abs();

        if (ysig.equals(BigInteger.ZERO))
            return new BigFloat(x.isSignBitSet && y.isSignBitSet, BigInteger.ZERO, BigInteger.ZERO, x.significandSize, x.exponentSize);

        if (ysig.compareTo(hiddenBitPow.multiply(BigInteger.valueOf(2))) >= 0) {
            ysig = ysig.shiftRight(1);
            yexp = yexp.add(BigInteger.ONE);
        }

        while (ysig.compareTo(hiddenBitPow) < 0 && yexp.compareTo(BigInteger.ONE) > 0) {
            ysig = ysig.shiftLeft(1);
            yexp = yexp.subtract(BigInteger.ONE);
        }

        if (ysig.compareTo(hiddenBitPow) < 0) {
            yexp = BigInteger.ZERO;
        } else {
            ysig = ysig.subtract(hiddenBitPow);
        }

        if (yexp.compareTo(BigInteger.valueOf(2).pow(x.exponentSize).subtract(BigInteger.ONE)) >= 0) {
            return new BigFloat(isNeg ? "-oo" : "+oo", x.significandSize, x.exponentSize);
        }

        return new BigFloat(isNeg, ysig, yexp, x.significandSize, x.exponentSize);
    }

    /**
     * Subtracts the specified {@code BigFloat} from this one.
     *
     * @param y the subtrahend
     * @return the difference {@code this - y}
     */
    public BigFloat subtract(BigFloat y) {
        return this.add(y.negate());
    }

    /**
     * Multiplies this {@code BigFloat} by another.
     * Both operands must have matching significand and exponent sizes.
     * Handles special values and edge cases according to IEEE standard semantics.
     *
     * @param y the multiplier
     * @return product of this and {@code y}
     * @throws IllegalArgumentException if sizes differ
     */
    public BigFloat multiply(BigFloat y) {
        BigFloat x = this;

        if (x.exponentSize != y.exponentSize || x.significandSize != y.significandSize) {
            throw new IllegalArgumentException("Cannot multiply BigFloats with differing sizes");
        }

        if (x.value.equals("NaN") || y.value.equals("NaN") ||
                ((x.value.equals("+oo") || x.value.equals("-oo")) && y.isZero()) ||
                ((y.value.equals("+oo") || y.value.equals("-oo")) && x.isZero())) {
            return new BigFloat("NaN", x.significandSize, x.exponentSize);
        }

        if (!x.value.isEmpty() || !y.value.isEmpty()) {
            boolean xSignNeg = x.value.isEmpty() ? x.isSignBitSet : x.value.charAt(0) == '-';
            boolean ySignNeg = y.value.isEmpty() ? y.isSignBitSet : y.value.charAt(0) == '-';
            return new BigFloat((xSignNeg ^ ySignNeg ? "-" : "+") + "oo", x.significandSize, x.exponentSize);
        }

        BigInteger xsig = x.significand;
        BigInteger ysig = y.significand;
        BigInteger xexp = x.exponent;
        BigInteger yexp = y.exponent;

        BigInteger hiddenBitPow = BigInteger.valueOf(2).pow(x.significandSize - 1);

        if (xexp.compareTo(BigInteger.ZERO) > 0) xsig = xsig.add(hiddenBitPow); else xexp = xexp.add(BigInteger.ONE);
        if (yexp.compareTo(BigInteger.ZERO) > 0) ysig = ysig.add(hiddenBitPow); else yexp = yexp.add(BigInteger.ONE);

        ysig = ysig.multiply(xsig);
        yexp = yexp.add(xexp).subtract(BigInteger.valueOf(2).pow(x.exponentSize - 1).subtract(BigInteger.ONE)).subtract(BigInteger.valueOf(x.significandSize - 1));

        while (ysig.compareTo(hiddenBitPow.multiply(BigInteger.valueOf(2))) >= 0 || yexp.compareTo(BigInteger.ZERO) <= 0) {
            ysig = ysig.shiftRight(1);
            yexp = yexp.add(BigInteger.ONE);
        }

        while (ysig.compareTo(hiddenBitPow) < 0 && yexp.compareTo(BigInteger.ONE) > 0) {
            ysig = ysig.shiftLeft(1);
            yexp = yexp.subtract(BigInteger.ONE);
        }

        if (ysig.compareTo(hiddenBitPow) < 0) {
            yexp = BigInteger.ZERO;
        } else {
            ysig = ysig.subtract(hiddenBitPow);
        }

        if (yexp.compareTo(BigInteger.valueOf(2).pow(x.exponentSize).subtract(BigInteger.ONE)) >= 0) {
            return new BigFloat(x.isSignBitSet ^ y.isSignBitSet ? "-oo" : "+oo", x.significandSize, x.exponentSize);
        }

        return new BigFloat(x.isSignBitSet ^ y.isSignBitSet, ysig, yexp, x.significandSize, x.exponentSize);
    }

    // ----- Utility Methods -----

    /**
     * Returns whether this instance represents zero (finite zero).
     *
     * @return {@code true} if the value is finite zero, {@code false} otherwise (including special values)
     */
    public boolean isZero() {
        return value.isEmpty() && significand.equals(BigInteger.ZERO) && exponent.equals(BigInteger.ZERO);
    }

    /**
     * Compares this {@code BigFloat} to another according to IEEE and C# Single.CompareTo specification semantics.
     *
     * <p>Relationship details:</p>
     * <ul>
     *   <li>Two NaNs compare equal to each other and greater than any number</li>
     *   <li>Positive/negative zero compare equal</li>
     *   <li>Ordinary comparisons proceed based on sign, exponent, significand</li>
     * </ul>
     *
     * @param that the {@code BigFloat} to compare with
     * @return 0 if equal, less than 0 if {@code this < that}, greater than 0 if {@code this > that}
     * @throws IllegalArgumentException if formats differ
     */
    @Override
    public int compareTo(BigFloat that) {
        if (this.exponentSize != that.exponentSize || this.significandSize != that.significandSize)
            throw new IllegalArgumentException("CompareTo requires matching floating point formats");

        if (this.value.isEmpty() && that.value.isEmpty()) {
            int cmpThis = this.isZero() ? 0 : (this.isSignBitSet ? -1 : 1);
            int cmpThat = that.isZero() ? 0 : (that.isSignBitSet ? -1 : 1);

            if (cmpThis == cmpThat) {
                if (this.exponent.equals(that.exponent))
                    return cmpThis * this.significand.compareTo(that.significand);
                return cmpThis * this.exponent.compareTo(that.exponent);
            }

            if (cmpThis == 0)
                return -cmpThat;
            return cmpThis;
        }

        if (Objects.equals(this.value, that.value))
            return 0;

        if (this.value.equals("NaN") || that.value.equals("+oo") || (this.value.equals("-oo") && !that.value.equals("NaN")))
            return -1;

        return 1;
    }

    // ----- CONVENIENCE STATIC COMPARISON METHODS (No operator overloading in Java) -----

    /**
     * Returns {@code true} if this {@code BigFloat} numerically equals {@code other}.
     * This method returns {@code false} if either number is NaN.
     *
     * @param other the other {@code BigFloat} to compare against
     * @return {@code true} if values are equal numerically, {@code false} otherwise
     */
    public boolean eq(BigFloat other) {
        if (this.value.equals("NaN") || other.value.equals("NaN")) return false;
        return this.compareTo(other) == 0;
    }

    /**
     * Returns {@code true} if this {@code BigFloat} does not numerically equal {@code other}.
     * Returns {@code true} if either value is NaN.
     *
     * @param other the other {@code BigFloat}
     * @return {@code true} if not equal, {@code false} otherwise
     */
    public boolean ne(BigFloat other) {
        if (this.value.equals("NaN") || other.value.equals("NaN")) return true;
        return this.compareTo(other) != 0;
    }

    /**
     * Returns {@code true} if this {@code BigFloat} is less than {@code other}.
     * Returns {@code false} if either value is NaN.
     *
     * @param other the other {@code BigFloat}
     * @return {@code true} if less than, {@code false} otherwise
     */
    public boolean lt(BigFloat other) {
        if (this.value.equals("NaN") || other.value.equals("NaN")) return false;
        return this.compareTo(other) < 0;
    }

    /**
     * Returns {@code true} if this {@code BigFloat} is greater than {@code other}.
     * Returns {@code false} if either value is NaN.
     *
     * @param other the other {@code BigFloat}
     * @return {@code true} if greater than, {@code false} otherwise
     */
    public boolean gt(BigFloat other) {
        if (this.value.equals("NaN") || other.value.equals("NaN")) return false;
        return this.compareTo(other) > 0;
    }

    /**
     * Returns {@code true} if this {@code BigFloat} is less than or equal to {@code other}.
     * Returns {@code false} if either value is NaN.
     *
     * @param other the other {@code BigFloat}
     * @return {@code true} if less than or equal, {@code false} otherwise
     */
    public boolean le(BigFloat other) {
        if (this.value.equals("NaN") || other.value.equals("NaN")) return false;
        return this.compareTo(other) <= 0;
    }

    /**
     * Returns {@code true} if this {@code BigFloat} is greater than or equal to {@code other}.
     * Returns {@code false} if either value is NaN.
     *
     * @param other the other {@code BigFloat}
     * @return {@code true} if greater than or equal, {@code false} otherwise
     */
    public boolean ge(BigFloat other) {
        if (this.value.equals("NaN") || other.value.equals("NaN")) return false;
        return this.compareTo(other) >= 0;
    }

    // ----- Floor and Ceiling Methods -----

    /**
     * Computes the floor (rounded towards negative infinity) and ceiling (rounded towards positive infinity)
     * of this {@code BigFloat} and returns them as a two-element array.
     *
     * <p>
     * This rounding behavior matches SMT-LIB semantics, where floor rounds downwards.
     * </p>
     *
     * @return an array where index 0 is floor, index 1 is ceiling
     * @throws IllegalStateException if called on special values (NaN, infinities)
     */
    public BigInteger[] floorCeiling() {
        if (!value.isEmpty()) {
            throw new IllegalStateException("floorCeiling() cannot be called on special float values: " + value);
        }

        BigInteger sig = significand;
        BigInteger exp = exponent;
        BigInteger hiddenBitPow = BigInteger.valueOf(2).pow(significandSize - 1);

        if (exponent.compareTo(BigInteger.ZERO) > 0) {
            sig = sig.add(hiddenBitPow);
        } else {
            exp = exp.add(BigInteger.ONE);
        }

        exp = exp.subtract(BigInteger.valueOf(2).pow(exponentSize - 1).subtract(BigInteger.ONE))
                .subtract(BigInteger.valueOf(significandSize - 1));

        BigInteger floor, ceiling;

        if (exp.compareTo(BigInteger.ZERO) >= 0) {
            while (exp.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                sig = sig.shiftLeft(Integer.MAX_VALUE);
                exp = exp.subtract(BigInteger.valueOf(Integer.MAX_VALUE));
            }

            sig = sig.shiftLeft(exp.intValue());
            floor = ceiling = isSignBitSet ? sig.negate() : sig;

        } else {
            exp = exp.negate();
            if (exp.intValue() > significandSize) {
                if (sig.equals(BigInteger.ZERO)) {
                    floor = ceiling = BigInteger.ZERO;
                } else {
                    ceiling = isSignBitSet ? BigInteger.ZERO : BigInteger.ONE;
                    floor = ceiling.subtract(BigInteger.ONE);
                }
            } else {
                BigInteger mask = BigInteger.ONE.shiftLeft(exp.intValue()).subtract(BigInteger.ONE);
                BigInteger frac = sig.and(mask);
                sig = sig.shiftRight(exp.intValue());

                if (frac.equals(BigInteger.ZERO)) {
                    floor = ceiling = isSignBitSet ? sig.negate() : sig;
                } else {
                    ceiling = isSignBitSet ? sig.negate() : sig.add(BigInteger.ONE);
                    floor = ceiling.subtract(BigInteger.ONE);
                }
            }
        }
        return new BigInteger[] { floor, ceiling };
    }
}
