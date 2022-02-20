echo "Generating js protobuf definitions"
# protoc --js_out=import_style=commonjs,binary:. numseq.proto
# protoc -I=./ --java_out=./ numseq.proto

echo "Generating java protobuf definitions"
../../../protoc-3.19.4-osx-x86_64/bin/protoc -I=./ --java_out=../server/num-sequence-service/src/main/java/ numseq.proto