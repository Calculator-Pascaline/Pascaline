# Pascaline

A calculator for Android. Named after the Pascaline, the mechanical calculator Blaise Pascal built in 1642.

<p align="center">
  <img src="docs/screenshots/calculator.png" width="24%" alt="Factorization, exact roots and discriminant of x²+6x−3" />
  <img src="docs/screenshots/functions.png" width="24%" alt="Graph of sin(x) and cos(x)" />
  <img src="docs/screenshots/probability.png" width="24%" alt="Binomial distribution with P(X ≤ 2) = 0.3483 and its expected value explained" />
  <img src="docs/screenshots/probability-tree.png" width="24%" alt="Probability tree of 4 free throws with arrows to each number of baskets" />
</p>
<p align="center">
  <img src="docs/screenshots/statistics.png" width="24%" alt="Histogram of grades from 8 to 16" />
  <img src="docs/screenshots/regression.png" width="24%" alt="Exponential regression y = 1.096·e^(0.672·x) through seven points" />
  <img src="docs/screenshots/elements.png" width="24%" alt="Periodic table with carbon selected" />
  <img src="docs/screenshots/prices.png" width="24%" alt="Three offers compared by price per kilogram" />
</p>

## Features

### Calculator

- **Natural input**: powers, fractions, roots and integral bounds are typed in boxes, the way they
  are written on paper
- **Exact answers** next to approximations: `√(−5) = √5i ≈ 2.236i`, `Σ 1/n² = π²/6`
- **Calculus**: symbolic derivatives and antiderivatives, including integration by parts and
  partial fractions (`∫x²eˣdx = (x²−2x+2)eˣ+C`), definite integrals, sums and products, including
  infinite ones
- **Algebra**: expressions are simplified and polynomials expanded and factorized, with their
  roots and discriminant
- **Complex numbers** with a complex plane showing modulus, argument, real and imaginary parts
- **Units and constants**: results in the easiest unit to read (`10⁹ Hz = 1 GHz`)
- **Number info**: decimal, hexadecimal, binary and prime factors of integers

### Functions

- Expressions, Graph and Table pages, like the NumWorks Functions app
- Several functions in their own colors, zoom with buttons or by pinching one axis at a time

### Probability

- Ten laws: binomial, uniform, exponential, normal, chi-squared, Student, geometric, Poisson,
  Fisher and hypergeometric
- Type a, b or the probability right inside `P(X ≤ a) = p`: entering a probability finds the
  bound, and for discrete laws it gives the closest probability that can actually be reached
- **Expected value and variance** of every law, with the formula, your values and why it holds
- **Binomial explained**: a probability tree of coin flips or free throws, colored arrows from each
  number of successes to its outcomes, and Pascal's triangle for the coefficients

### Sequences

- As many sequences as you want, explicit or recursive (order 1 or 2), typed with the calculator
  keyboard and its uₙ and uₙ₊₁ keys
- Graph of the terms and a table of values

### Statistics and regression

- **Statistics**: values and frequencies, histogram, box plot, quartiles, mean and standard
  deviation
- **Regression**: eight kinds of curves fitted to your points, from linear to power, with the
  graph, the coefficients, r and r²

### Inference

- Tests and confidence intervals for one or two proportions and for one or two means (z or t),
  and the χ² goodness of fit test
- The statistic, degrees of freedom, p-value and conclusion at your significance level

### Finance

- Simple and compound interest: find any of N, I%, PV, Pmt and FV from the others, with payments
  and compoundings per year

### Elements

- Periodic table of the 118 elements, colored by family, with mass, electron configuration and
  electronegativity

### Price comparison

- Compare as many offers as you like by price per kg, L, m or piece, even across units like 500 g
  and 1.2 kg, and see how much the cheapest saves

### And also

- **Unit converter** with watt-hours, and long results that wrap instead of being cut off; date
  calculator, time zones and body mass index from Unitto
- **Your menu**: reorder the apps and hide the ones you don't use
- **Phone keypad**: put 1 2 3 on the top row, like a phone
- **Lock screen**: the app stays open when the phone locks, without unlocking the rest of it
- **Static cursor** that doesn't blink
- **No internet permission**: everything is computed on the device

## Credits

Pascaline is based on [Unitto](https://github.com/sadellie/unitto) by Elshan Agaev. Thanks for
building the foundation this app grew from. Pascaline is not affiliated with or endorsed by
Unitto.

## License

Pascaline is free software under the [GNU General Public License v3.0](./LICENSE), like Unitto.
Files changed from Unitto keep their original copyright notice next to the new one.
