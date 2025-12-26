all: check-protobuf-version libs libadb.a libadb.include

brew-install-deps:
	brew install fmt cmake pkgconfig googletest

check-protobuf-version:
	@grep $$(protoc --version | cut -d' ' -f2) porting/protobuf.rb || { echo "* To compile libadb please install protobuf version 28.3 by:" && echo "brew unlink protobuf && brew install porting/protobuf.rb" && echo "" && exit 1; }
	protoc --version
	exit 0

check-golang:
	@which go || { echo "* To compile libadb please install golang by execute:"; echo ""; }

libs:
	[[ -d output ]] || mkdir output && echo "mkdir output"
	make -C porting

libadb.include:
	[[ ! -d output/include ]] && mkdir output/include || echo "make include";
	cp -av porting/adb/include/*.h output/include;

libadb.a:
	rm -fv output/*/*/libadb-full.a || echo ""
	libtool -static -o output/iphoneos/arm64/libadb-full.a output/iphoneos/arm64/*.a
	libtool -static -o output/iphonesimulator/x86_64/libadb-full.a output/iphonesimulator/x86_64/*.a
	libtool -static -o output/iphonesimulator/arm64/libadb-full.a output/iphonesimulator/arm64/*.a
