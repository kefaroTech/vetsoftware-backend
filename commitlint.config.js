module.exports = {
  extends: ['gitmoji'],
  rules: {
    'scope-case': [2, 'always', 'lower-case'],
    'subject-max-length': [2, 'always', 72],
    'subject-empty': [2, 'never'],
    'type-empty': [2, 'never'],
  },
};
