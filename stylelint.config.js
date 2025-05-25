export default {
  extends: ["stylelint-config-standard-scss"],
  plugins: [],
  ignoreFiles: ["build/**", "node_modules/**"],
  rules: {
    "font-family-name-quotes": null,
    "declaration-empty-line-before": null,
    "alpha-value-notation": "number",
    "color-hex-length": "long",
    "value-keyword-case": null,
  },
};
