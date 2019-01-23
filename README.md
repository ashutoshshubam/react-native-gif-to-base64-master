
# react-native-react-native-gif-base64

## Getting started

`$ npm install --save https://github.com/rohitpathak88/react-native-gif-to-base64-master`

### Mostly automatic installation

`$ react-native link react-native-react-native-gif-base64`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-react-native-gif-base64` and add `RNReactNativeGifBase64.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNReactNativeGifBase64.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.reactlibrary.RNReactNativeGifBase64Package;` to the imports at the top of the file
  - Add `new RNReactNativeGifBase64Package()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-react-native-gif-base64'
  	project(':react-native-react-native-gif-base64').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-react-native-gif-base64/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-react-native-gif-base64')
  	```
## Usage
```javascript
import RNReactNativeGifBase64 from 'react-native-react-native-gif-base64';

    let gifArr = require('./test/data.json');
    let facesArr = require('./test/faces.json');

    const data =  {
      'gifArr':gifArr ,
      'faceArr': facesArr
    };

    RNReactNativeGifBase64.getBase64String(data, (response) => {
      // Same code as in above section!
      console.log(response);
    });
    
```
  
