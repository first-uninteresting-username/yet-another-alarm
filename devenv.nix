{ pkgs, ... }:

{
  android = {
    enable = true;
    platforms.version = [ "37-2" ];
    buildTools.version = [ "37.0.0" ];
    emulator.enable = false;
    ndk.enable = false;
    android-studio.enable = false;
  };

  languages.java.jdk.package = pkgs.jdk21;
}
