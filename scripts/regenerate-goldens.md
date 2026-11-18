# Regenerating the golden files from the original code

The expected values in `converter/src/test/resources/golden` come from the original 2018 code
(commit `fafa508`), not from the current code. Regenerate them only to add new sample images or
new recorded fields; when you change behaviour on purpose, edit the affected golden lines instead.

The 2018 code no longer builds as-is: its Maven repositories are gone, and two libraries
(tomgibara imageio-tiff, icafe 1.1-SNAPSHOT) can't be downloaded anywhere. `golden-baseline.patch`
holds the build-only changes that make it compile. None of them touch the conversion logic:

- Repository URLs: http → https, plus the OSGeo repository for JAI and Spring's milestone repository.
- `com.tomgibara.imageio` → `com.github.jaiimageio`: the same Sun TIFF plugin under a different package.
  Compression is lossless, so the decoded pixels are unaffected.
- `VicarThreadedEXIFConverter` becomes a no-op, because icafe is gone. The harness records the EXIF
  values the old code computed, and `ExifTagsGoldenTest` checks them against the new writer's output.

There is no JDK 8 on the original build machine anymore, so the baseline runs on JDK 17 at Java 8
language level. The calibrators only use sqrt/abs/round, which are bit-exact on every JVM.

## Steps

```sh
git worktree add --detach ../isr-baseline fafa508
cd ../isr-baseline
git apply ../isr/scripts/golden-baseline.patch
# the harness is Java 8 compatible; copy it and the unit tests in
cp -R ../isr/converter/src/test/java/com/astrodoorways/converter/golden converter/src/test/java/com/astrodoorways/converter/
rm converter/src/test/java/com/astrodoorways/converter/golden/ExifTagsGoldenTest.java   # needs Java 9+ APIs
cp -R ../isr/converter/src/test/resources/test-dirs converter/src/test/resources/       # if you added samples

export JAVA_HOME=$(/usr/libexec/java_home -v 17)
mvn -B test -pl converter -am \
  -Dtest='ImageGoldenMasterTest,PipelineGoldenMasterTest' -DfailIfNoTests=false \
  -DargLine="-Duser.timezone=UTC -Duser.language=en -Duser.country=US -Xmx4g \
    --add-opens java.base/java.lang=ALL-UNNAMED \
    -Dgolden.generate=true -Disr.calib.dir=$PWD/../isr-calib/coiss_0011_v3"

cp -R converter/src/test/resources/golden ../isr/converter/src/test/resources/
cd ../isr && git worktree remove ../isr-baseline
```

Build with `-pl converter -am` (a reactor build) so the baseline uses its own module jars and not
the newer ones in `~/.m2`, which have the same version numbers.
