module.exports = {
  dependency: {
    platforms: {
      android: {
        sourceDir: './android',
        packageImportPath: 'import com.rnstorybookautoscreenshots.RNStorybookAutoScreenshotsPackage;',
        packageInstance: 'new RNStorybookAutoScreenshotsPackage()',
      },
      ios: null, // iOS not supported yet
    },
  },
};
