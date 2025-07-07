module.exports = {
  moduleFileExtensions: ['js', 'json', 'ts'],
  rootDir: 'src',
  testRegex: '.*\\.spec\\.ts$',
  transform: {
    '^.+\\.(t|j)s$': 'ts-jest',
  },
  collectCoverageFrom: [
    '**/*.(t|j)s',
  ],
  coverageDirectory: '../coverage',
  testEnvironment: 'node',
  testPathIgnorePatterns: [
    '<rootDir>/unified-sdk/__tests__/',
    '<rootDir>/unified-sdk/src/__tests__/',
    '<rootDir>/unified-sdk/dist/',
  ],
  modulePathIgnorePatterns: [
    '<rootDir>/unified-sdk/__tests__/',
    '<rootDir>/unified-sdk/src/__tests__/',
    '<rootDir>/unified-sdk/dist/',
  ],
}; 