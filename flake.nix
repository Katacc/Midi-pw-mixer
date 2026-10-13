{
  description = "Java development flake";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable";
  };

  outputs =
    {
      self,
      nixpkgs,
      ...
    }:
    let
      pkgs = import nixpkgs {
        system = "x86_64-linux";
        config = {
          allowUnfree = true;
        };
      };
    in
    {
      devShells."x86_64-linux".default = pkgs.mkShell rec {
        JAVA_HOME = "${pkgs.jdk}";
        QT_QPA_PLATFORM = "wayland;xcb"; # Comment if using X11!

        buildInputs = with pkgs; [
          jdk
          gradle
          maven
          gtk3

          jetbrains.idea-community
        ];

        shellHook = ''
          echo "Java Development Environment"
          echo "=============================="
          echo "JAVA_HOME: $JAVA_HOME"
          echo ""

        '';
      };
    };
}
