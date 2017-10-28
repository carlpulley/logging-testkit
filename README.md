# System Testkit

TODO:

# Publishing and Release

Once we have merged a set of feature branches into the `master` branch and are ready to cut a release:
```
git hf release start X.Y.Z
# Edit version.sbt so that version == X.Y.Z
# Commit changes to the release branch
git hf release push
# Ensure that Travis CI passes the build
git hf release finish X.Y.Z
sbt clean ghpagesPushSite
sbt "+ publishSigned"
sbt sonatypeRelease
```

Code should now be available from Maven Central at version X.Y.Z.
