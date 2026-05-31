import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.yyjapi.app',
  appName: '优易接API',
  webDir: 'www',
  plugins: {
    App: {
      disableBackButtonHandler: true
    },
    SplashScreen: {
      launchShowDuration: 300,
      backgroundColor: '#ffffff',
      showSpinner: false
    }
  },
  server: {
    url: 'https://www.yyjapi.com',
    cleartext: false,
    androidScheme: 'https',
    allowNavigation: [
      'yyjapi.com',
      'www.yyjapi.com',
      'cf.yyjapi.com'
    ]
  }
};

export default config;
