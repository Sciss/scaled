//
// Scaled - a scalable editor extensible via JVM languages
// http://github.com/scaled/scaled/blob/master/LICENSE

package scaled.pacman;

import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads classes for a particular package. A package has two kinds of dependencies: Maven
 * dependencies, which are private to the package, and will be searched first, and Package
 * dependencies, wherein a Scaled package depends on another Scaled package.
 */
public class PackageLoader extends URLClassLoader {

  public final Source source;
  public final Path classes;
  public final Collection<Path> mavenDeps;
  public final Iterable<PackageLoader> packageDeps;

  public PackageLoader (Source source, Path classes, Collection<Path> mavenDeps,
                        Iterable<PackageLoader> packageDeps) {
    super(toURLs(classes, mavenDeps));
    this.source = source;
    this.classes = classes;
    this.mavenDeps = mavenDeps;
    this.packageDeps = packageDeps;
  }

  public void accumMavenDeps (Set<Path> into) {
    into.addAll(mavenDeps);
    for (PackageLoader dep : packageDeps) dep.accumMavenDeps(into);
  }

  public List<Path> classpath () {
    List<Path> cp = new ArrayList<>();
    Set<Source> seen = new HashSet<Source>();
    buildClasspath(cp, seen);
    return cp;
  }

  public void dump (PrintStream out, String indent, Set<Source> seen) {
    if (seen.add(source)) {
      out.println(indent + source);
      out.println(indent + "= " + classes);
      String dindent = indent + "- ";
      for (Path path : mavenDeps) out.println(dindent + path);
      for (PackageLoader pkg : packageDeps) pkg.dump(out, dindent, seen);
    } else {
      out.println(indent + "(*) " + source);
    }
  }

  private void buildClasspath (List<Path> cp, Set<Source> seen) {
    if (seen.add(source)) {
      cp.add(classes);
      cp.addAll(mavenDeps);
      for (PackageLoader dep : packageDeps) dep.buildClasspath(cp, seen);
    }
  }

  @Override public URL getResource (String path) {
    URL rsrc = super.getResource(path);
    if (rsrc != null) return rsrc;
    for (PackageLoader loader : packageDeps) {
      URL drsrc = loader.getResource(path);
      if (drsrc != null) return drsrc;
    }
    return null;
  }

  @Override protected Class<?> findClass (String name) throws ClassNotFoundException {
    // System.err.println("Seeking "+ name +" in "+ source);
    try { return super.findClass(name); }
    catch (ClassNotFoundException cnfe) {} // check our package deps
    for (PackageLoader loader : packageDeps) {
      try { return loader.loadClass(name); }
      catch (ClassNotFoundException cnfe) {} // keep going
    }
    throw new ClassNotFoundException(source + " missing dependency: " + name);
  }

  @Override public String toString () {
    return "PkgLoader(" + source + ")";
  }

  private static URL[] toURLs (Path classes, Collection<Path> paths) {
    URL[] urls = new URL[1+paths.size()];
    int ii = 0;
    urls[ii++] = toURL(classes);
    for (Path path : paths) urls[ii++] = toURL(path);
    return urls;
  }

  private static URL toURL (Path path) {
    try { return path.toUri().toURL(); }
    catch (MalformedURLException e) { throw new AssertionError(e); }
  }
}
