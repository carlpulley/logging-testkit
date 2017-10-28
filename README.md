# Logging Testkit

TODO:

# Publishing and Releasing

Once we have merged a set of feature branches into the `master` branch and are ready to cut a release:
```
git tag -s vX.Y.Z
git push -tags
sbt ghpagesPushSite
sbt "+ publishSigned"
sbt sonatypeRelease
```

Code should now be available from Maven Central at version X.Y.Z.
