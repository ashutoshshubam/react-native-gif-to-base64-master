using ReactNative.Bridge;
using System;
using System.Collections.Generic;
using Windows.ApplicationModel.Core;
using Windows.UI.Core;

namespace React.Native.Gif.Base64.RNReactNativeGifBase64
{
    /// <summary>
    /// A module that allows JS to share data.
    /// </summary>
    class RNReactNativeGifBase64Module : NativeModuleBase
    {
        /// <summary>
        /// Instantiates the <see cref="RNReactNativeGifBase64Module"/>.
        /// </summary>
        internal RNReactNativeGifBase64Module()
        {

        }

        /// <summary>
        /// The name of the native module.
        /// </summary>
        public override string Name
        {
            get
            {
                return "RNReactNativeGifBase64";
            }
        }
    }
}
