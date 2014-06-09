//
// Scaled - a scalable editor extensible via JVM languages
// http://github.com/samskivert/scaled/blob/master/LICENSE

package scaled.pacman

/** Contains runtime metadata for an installed package. */
class Package (mgr :PackageManager, val info :PackageInfo) {

  /** The class loader for classes in this package. */
  val loader :ClassLoader = new PackageLoader(info.name, info.classesDir.toUri.toURL) {
    override protected def resolveDependLoaders = info.depends.flatMap(mgr.resolveDepend(info))
  }

  override def toString = info.name
}
