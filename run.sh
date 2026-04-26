#!/bin/bash

# Script'in bulunduğu dizine geç
cd "$(dirname "$0")"

# Maven'ın yüklü olup olmadığını kontrol et
if ! command -v mvn &> /dev/null
then
    echo "HATA: Maven sisteminizde bulunamadı."
    echo "Projeyi derlemek ve çalıştırmak için Maven ve JDK 17+ gereklidir."
    echo "macOS için Homebrew kullanarak kolayca yükleyebilirsiniz:"
    echo "brew install maven"
    exit 1
fi

echo "Projeyi derliyor ve başlatıyorum..."
mvn clean compile exec:java
