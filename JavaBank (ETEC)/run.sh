#!/bin/sh
cd "$(dirname "$0")"
FX="${JAVAFX_HOME:-javafx-lib}"
mkdir -p out
javac --module-path "$FX" --add-modules javafx.controls -cp postgresql-42.7.13.jar -d out Database.java App.java || exit 1
java --enable-native-access=javafx.graphics --module-path "$FX" --add-modules javafx.controls -cp "out:postgresql-42.7.13.jar" App
