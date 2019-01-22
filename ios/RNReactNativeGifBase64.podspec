
Pod::Spec.new do |s|
  s.name         = "RNReactNativeGifBase64"
  s.version      = "1.0.0"
  s.summary      = "RNReactNativeGifBase64"
  s.description  = <<-DESC
                  RNReactNativeGifBase64
                   DESC
  s.homepage     = ""
  s.license      = "MIT"
  # s.license      = { :type => "MIT", :file => "FILE_LICENSE" }
  s.author             = { "author" => "author@domain.cn" }
  s.platform     = :ios, "7.0"
  s.source       = { :git => "https://github.com/author/RNReactNativeGifBase64.git", :tag => "master" }
  s.source_files  = "RNReactNativeGifBase64/**/*.{h,m}"
  s.requires_arc = true


  s.dependency "React"
  #s.dependency "others"

end

  