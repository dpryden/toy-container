package net.pryden.toy.container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import net.pryden.toy.container.ToyContainer.InjectionException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.InvocationTargetException;

/**
 * Tests for {@link ToyContainer}.
 */
@RunWith(JUnit4.class)
public class ToyContainerTest {

  private static interface SimpleMarkerInterface {}

  private static interface Fooable extends SimpleMarkerInterface {
    String getBazValue();
    String getBarBazValue();
  }

  private static class Foo implements Fooable {
    final Bar bar;
    final Baz baz;
    
    public Foo(Bar bar, Baz baz) {
      this.bar = bar;
      this.baz = baz;
    }
    
    @Override
    public String getBazValue() {
      return baz.value;
    }
    
    @Override
    public String getBarBazValue() {
      return bar.baz.value;
    }
  }
  
  private static class Bar {
    final Baz baz;
    
    public Bar(Baz baz) {
      this.baz = baz;
    }
  }
  
  private static class Baz {
    final String value;
    
    public Baz(String value) {
      this.value = value;
    }
  }
  
  private static class Bogus {
    public Bogus(Baz baz) {
      throw new IllegalArgumentException("oh no you didn't");
    }
  }

  private static class MultipleConstructors {
    public MultipleConstructors(Bogus bogus) {}
    public MultipleConstructors(int a, int b, long c) {}
  }

  @Test
  public void trivialInjection() {
    ToyContainer injector = new ToyContainer();
    String value = "hello world";
    injector.bind(Baz.class, new Baz(value));
    Baz baz = injector.resolve(Baz.class);
    assertEquals(value, baz.value);
  }
  
  @Test
  public void constructorInjection() {
    ToyContainer injector = new ToyContainer();
    String value = "hi";
    injector.bind(Baz.class, new Baz(value));
    Bar bar = injector.resolve(Bar.class);
    assertEquals(value, bar.baz.value);
  }
  
  @Test
  public void whenDependencyIsNotBoundAndCannotBeConstructed_throwsInjectionException() {
    ToyContainer injector = new ToyContainer();
    try {
      injector.resolve(Baz.class);
      fail("Expected InjectionException");
    } catch (InjectionException expected) {}
  }
  
  @Test
  public void recursiveDependencyInjection() {
    ToyContainer injector = new ToyContainer();
    String value = "xyzzy";
    injector.bind(Baz.class, new Baz(value));
    Foo foo = injector.resolve(Foo.class);
    assertEquals(value, foo.getBazValue());
    assertEquals(value, foo.getBarBazValue());
  }
  
  @Test
  public void bindingInterfaceToConcreteType() {
    ToyContainer injector = new ToyContainer();
    String value = "qwerty";
    injector.bind(Baz.class, new Baz(value));
    injector.bind(Fooable.class, Foo.class);
    Fooable foo = injector.resolve(Fooable.class);
    assertEquals(value, foo.getBazValue());
    assertEquals(value, foo.getBarBazValue());
  }
  
  @Test
  public void recursiveInterfaceBinding_whenCannotResolveConcreteType_throwsInjectionException() {
    ToyContainer injector = new ToyContainer();
    injector.bind(SimpleMarkerInterface.class, Fooable.class);
    try {
      injector.resolve(SimpleMarkerInterface.class);
      fail("Expected InjectionException");
    } catch (InjectionException expected) {}
  }
  
  @Test
  public void recursiveInterfaceBindings() {
    ToyContainer injector = new ToyContainer();
    injector.bind(Baz.class, new Baz("whoa!"));
    injector.bind(Fooable.class, Foo.class);
    injector.bind(SimpleMarkerInterface.class, Fooable.class);
    SimpleMarkerInterface foo = injector.resolve(SimpleMarkerInterface.class);
    assertTrue("Expected SimpleMarkerInterface instance to be Foo",
        foo instanceof Foo);
  }
  
  @Test
  public void whenConstructorThrowsException_throwsInjectionException() {
    ToyContainer injector = new ToyContainer();
    injector.bind(Baz.class, new Baz("never used"));
    try {
      injector.resolve(Bogus.class);
      fail("Expected InjectionException");
    } catch (InjectionException caught) {
      assertTrue("Expected cause to be an InvocationTargetException",
          caught.getCause() instanceof InvocationTargetException);
      assertTrue("Expected root cause to be an IllegalArgumentException",
          caught.getCause().getCause() instanceof IllegalArgumentException);
    }
  }
  
  @Test
  public void multipleConstructors_throwsInjectionException() {
    ToyContainer injector = new ToyContainer();
    try {
      injector.resolve(MultipleConstructors.class);
      fail("Expected InjectionException");
    } catch (InjectionException expected) {}
  }
}