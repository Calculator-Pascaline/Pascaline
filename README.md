<p align="center">
  <img src="docs/icon-round.png" width="120" alt="Pascaline icon" />
</p>

<h1 align="center">Pascaline</h1>

<p align="center">
  A calculator for Android and the web.<br />
  <sub>Named after the Pascaline, the mechanical calculator Blaise Pascal built in 1642.</sub>
</p>

<p align="center">
  <a href="https://github.com/Calculator-Pascaline/Pascaline/releases/latest"><img src="docs/buttons/download-android.svg" height="44" alt="Download for Android" /></a>&nbsp;&nbsp;<a href="https://calculator-pascaline.github.io/Pascaline/"><img src="docs/buttons/open-web-app.svg" height="44" alt="Open the web app" /></a>
</p>

## 📲 Install

<table>
  <tr>
    <th width="33%">Android app</th>
    <th width="33%">Web app on Android</th>
    <th width="33%">Web app on iPhone &amp; iPad</th>
  </tr>
  <tr valign="top">
    <td>
      1. Download the <code>.apk</code> from the
      <a href="https://github.com/Calculator-Pascaline/Pascaline/releases/latest">latest release</a><br />
      2. Open it, and allow the install if Android asks<br />
      3. Tap <b>Install</b>
    </td>
    <td>
      1. Open the <a href="https://calculator-pascaline.github.io/Pascaline/">web app</a> in Chrome<br />
      2. Tap <b>⋮</b>, then <b>Add to Home screen</b><br />
      3. Choose <b>Install</b>
    </td>
    <td>
      1. Open the <a href="https://calculator-pascaline.github.io/Pascaline/">web app</a> in Safari<br />
      2. Tap <b>Share</b> (the square with an arrow)<br />
      3. Choose <b>Add to Home Screen</b>, then <b>Add</b>
    </td>
  </tr>
</table>

<p align="center"><sub>Added to the home screen, the web app opens in its own window and works without internet.</sub></p>

<details>
<summary>Check that your <code>.apk</code> is the real one</summary>
<br />

Every release is signed with the same certificate. Its SHA-256 fingerprint is:

```
A4:A9:81:99:0C:02:D0:CB:78:FF:EE:07:14:F5:25:C6:A2:72:22:E4:94:FF:CC:32:AE:12:DA:74:F7:21:4C:AE
```

Apps like [AppVerifier](https://github.com/soupslurpr/AppVerifier) show it for an installed app, or run `apksigner verify --print-certs` on the `.apk`. A different fingerprint means someone else built and signed that file.

</details>

## ✨ Features

### 🏠 Every tool on one screen

<table>
  <tr>
    <td align="center" valign="top" width="33%">
      <img src="docs/screenshots/home.png" width="240" alt="Home with a search box, the recent tools, then the Math and Everyday sections with Geometry, 3D graphs, Inflation and Currency changeover" /><br />
    </td>
    <td valign="top">
      Home lists every tool, grouped by topic: <b>Math</b>, <b>Everyday</b>, <b>Data</b>, <b>Science</b> and <b>Electronics</b>.<br /><br />
      Search a tool by its name, or find the ones you used last at the top.<br /><br />
      In the edit mode, rename, move and add sections, drag tools to change their place, move them to another section, and hide the ones you don't need.
    </td>
  </tr>
</table>

### 🧮 Advanced math

<table>
  <tr>
    <td align="center" valign="top" width="33%">
      <b>Complex numbers</b><br />
      <img src="docs/screenshots/complex-numbers.png" width="240" alt="(1+2i)² is −3+4i, √−5 is √5 i, and 1+i on the complex plane with length √2 and angle π/4" /><br />
      <sub>Exact results and the complex plane</sub>
    </td>
    <td align="center" valign="top" width="33%">
      <b>Calculus</b><br />
      <img src="docs/screenshots/calculus.png" width="240" alt="The integral of x² is x³/3 + C, a derivative, and the integral of 1/(1+x²) from −∞ to ∞ is π" /><br />
      <sub>Integrals, even to infinity, and derivatives</sub>
    </td>
    <td align="center" valign="top" width="33%">
      <b>Series</b><br />
      <img src="docs/screenshots/series.png" width="240" alt="The sum of 1/n² from n=1 to ∞ is π²/6, about 1.645" /><br />
      <sub>Sums and products, with exact values</sub>
    </td>
  </tr>
</table>

### 📐 Algebra and functions

<table>
  <tr>
    <td align="center" valign="top" width="33%">
      <b>Equations</b><br />
      <img src="docs/screenshots/equations.png" width="240" alt="x⁵+6x factorized, with the roots x≈0, x≈−1.107±1.107i and x≈1.107±1.107i" /><br />
      <sub>Factorization and all the roots</sub>
    </td>
    <td align="center" valign="top" width="33%">
      <b>Functions</b><br />
      <img src="docs/screenshots/functions.png" width="240" alt="The graphs of x² and sin(x) with their zeros" /><br />
      <sub>Graphs, zeros, derivatives and tangents</sub>
    </td>
    <td align="center" valign="top" width="33%">
      <b>Programmer</b><br />
      <img src="docs/screenshots/programmer.png" width="240" alt="FF AND 0F is F, shown in hexadecimal, decimal, octal and binary" /><br />
      <sub>HEX, DEC, OCT and BIN with bitwise operators</sub>
    </td>
  </tr>
</table>

### 🧊 In 3D and in the plane

<table>
  <tr>
    <td align="center" valign="top" width="33%">
      <b>Multiple integrals</b><br />
      <img src="docs/screenshots/multiple-integrals.png" width="240" alt="The double integral of 4 − x² − y² over the unit disk drawn as a volume in 3D, equal to 10.9957" /><br />
      <sub>Double and triple integrals in 3D you turn with a finger, with bounds that depend on x and y</sub>
    </td>
    <td align="center" valign="top" width="33%">
      <b>3D graphs</b><br />
      <img src="docs/screenshots/3d-graphs.png" width="240" alt="The level curves of x³ − 3x + y² with the gradient arrows, a minimum at (1, 0) and a saddle point at (−1, 0)" /><br />
      <sub>Surfaces in 3D or as level curves, with ∇f, div and rot, and the critical points</sub>
    </td>
    <td align="center" valign="top" width="33%">
      <b>Geometry</b><br />
      <img src="docs/screenshots/geometry.png" width="240" alt="A triangle from a = 7, b = 5 and C = 60°, drawn to scale, with its sides, angles, area, perimeter and heights" /><br />
      <sub>Triangles from any three values, quadrilaterals, circles and solids</sub>
    </td>
  </tr>
</table>

### 📊 Data

<table>
  <tr>
    <td align="center" valign="top" width="33%">
      <b>Statistics</b><br />
      <img src="docs/screenshots/statistics.png" width="240" alt="Values with their counts, the mean, median and standard deviation, and a histogram with a box plot" /><br />
      <sub>Histograms, box plots and every statistic</sub>
    </td>
    <td align="center" valign="top" width="33%">
      <b>Regression</b><br />
      <img src="docs/screenshots/regression.png" width="240" alt="Points with the line y = 2.02x − 0.02 through them, explaining 99.8% of how y changes, and a prediction" /><br />
      <sub>Drag your points, the best curve follows</sub>
    </td>
    <td align="center" valign="top" width="33%">
      <b>Inference</b><br />
      <img src="docs/screenshots/inference.png" width="240" alt="A one proportion test with the two bell curves, the p-value shaded, and the verdict A real difference" /><br />
      <sub>Tests and intervals, with both distributions and a plain answer</sub>
    </td>
  </tr>
  <tr>
    <td align="center" valign="top" width="33%">
      <b>Probability</b><br />
      <img src="docs/screenshots/probability.png" width="240" alt="P(X ≤ 184.3) is 0.908 for heights with a mean of 175 and a standard deviation of 7, with the line dragged on the bell curve" /><br />
      <sub>Drag the graph to get the probability, with examples</sub>
    </td>
    <td></td>
    <td></td>
  </tr>
</table>

### 🔭 Science

<table>
  <tr>
    <td align="center" valign="top" width="33%">
      <b>Sky and Earth</b><br />
      <img src="docs/screenshots/sky-and-earth.png" width="240" alt="Sunset at 4:23 PM in London, the height of the sun through the day, and the twilights" /><br />
      <sub>Sun, moon, sky, tides, temperature and rain for any city, offline</sub>
    </td>
    <td align="center" valign="top" width="33%">
      <b>Projectile</b><br />
      <img src="docs/screenshots/projectile.png" width="240" alt="An animated throw reaching 66.69 m with its highest point at 23.69 m" /><br />
      <sub>An animated throw, with air, and how it is calculated</sub>
    </td>
    <td align="center" valign="top" width="33%">
      <b>Elements</b><br />
      <img src="docs/screenshots/elements.png" width="240" alt="Sulfur with its atom, its place in the periodic table and its details" /><br />
      <sub>The periodic table, with an animated atom</sub>
    </td>
  </tr>
  <tr>
    <td align="center" valign="top" width="33%">
      <b>Gears</b><br />
      <img src="docs/screenshots/gears.png" width="240" alt="A train of four gears with two on one shaft, a 6 : 1 ratio, 250 rpm out, the torque, efficiency and power" /><br />
      <sub>Trains, worms, racks, planetary sets and a ratio finder</sub>
    </td>
    <td></td>
    <td></td>
  </tr>
</table>

### ⚡ Electronics

<table>
  <tr>
    <td align="center" valign="top" width="33%">
      <b>Filters</b><br />
      <img src="docs/screenshots/filters.png" width="240" alt="A band-pass RLC filter with its circuit, its gain and phase curves, and the output against the input" /><br />
      <sub>RC, RL and RLC filters with their circuit and Bode plot</sub>
    </td>
    <td align="center" valign="top" width="33%">
      <b>Charging</b><br />
      <img src="docs/screenshots/charging.png" width="240" alt="A capacitor charging through a resistor, with its circuit and its curve over five time constants" /><br />
      <sub>Capacitors, coils and the 555 timer</sub>
    </td>
    <td align="center" valign="top" width="33%">
      <b>Resistor colors</b><br />
      <img src="docs/screenshots/resistor-colors.png" width="240" alt="A 470 Ω resistor with its color bands, and the nearest standard values" /><br />
      <sub>Read the bands, or find a standard value</sub>
    </td>
  </tr>
</table>

### 🧰 Everyday tools

<table>
  <tr>
    <td align="center" valign="top" width="33%">
      <b>Unit converter</b><br />
      <img src="docs/screenshots/units.png" width="240" alt="100 kilometers is 62.137 miles" /><br />
      <sub>Convert between all sorts of units</sub>
    </td>
    <td align="center" valign="top" width="33%">
      <b>Finance</b><br />
      <img src="docs/screenshots/finance.png" width="240" alt="A loan of 200 000 at 5% over 20 years: 1 319.91 a month, with what is repaid and the interest year by year" /><br />
      <sub>Loans, savings, and simple and compound interest</sub>
    </td>
    <td align="center" valign="top" width="33%">
      <b>Currency changeover</b><br />
      <img src="docs/screenshots/currency-changeover.png" width="240" alt="1 000 French francs in 1995 are worth about 244.98 € today with inflation, 152.45 € at the fixed rate, with the timeline of the franc" /><br />
      <sub>Old money in euros, with what it was worth then and now</sub>
    </td>
  </tr>
  <tr>
    <td align="center" valign="top" width="33%">
      <b>And more</b><br />
      <sub>Sequences, Orbits, Logic gates, Inflation, Price comparison, Date calculator and Body mass</sub>
    </td>
    <td></td>
    <td></td>
  </tr>
</table>

## 🌍 23 languages

Arabic, Bengali, Chinese, Czech, Dutch, English, French, German, Hindi, Hungarian, Indonesian, Italian, Japanese, Korean, Persian, Polish, Portuguese, Russian, Slovak, Spanish, Turkish, Ukrainian and Vietnamese. Arabic and Persian read from right to left, while the keypads and the math stay left to right.

## 🔒 Privacy

The Android app doesn't ask for the internet permission, so it can't connect to the internet: your calculations stay on your device. Links like **Report an issue** open in your browser.

The web app is downloaded from GitHub Pages, then everything you do in it stays in your browser.

## 💬 Feedback

Ideas, bugs or anything that could make Pascaline better are welcome.

<p>
  <a href="https://github.com/Calculator-Pascaline/Pascaline/issues/new"><img src="docs/buttons/open-issue.svg" height="44" alt="Open an issue" /></a>
</p>

## 🙏 Credits

Pascaline is based on [Unitto](https://github.com/sadellie/unitto) by Elshan Agaev. Thanks for building the foundation this app grew from. Pascaline is not affiliated with or endorsed by Unitto.

How much comes from each, as of version 1.8.0, in lines of Kotlin code. Only real code is counted: license headers, blank lines, comments, the bundled math library (`kt-math`), the icon drawings and the translations are left out.

| | From Unitto | From Pascaline |
|---|---|---|
| All code | 24,256 lines (36.9%) | 41,423 lines (63.1%) |
| Calculator and its tools | 1,362 lines (5.0%) | 25,671 lines (95.0%) |
| Math engine | 511 lines (9.5%) | 4,854 lines (90.5%) |

Unitto's code is mostly the base underneath: storage, settings, the unit converter and the home screen widget. The calculator, its math engine and the tools are mostly Pascaline's. Each line is counted for whoever wrote it (`git blame -w -M -C`): a Unitto line that was only reformatted or moved still counts as Unitto's.

## 📄 License

Pascaline is free software under the [GNU General Public License v3.0](./LICENSE), like Unitto. Files changed from Unitto keep their original copyright notice next to the new one.
