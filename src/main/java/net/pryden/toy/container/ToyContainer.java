package net.pryden.toy.container;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * A simple dependency injection container made as a toy.
 *
 * <p>Sample usage:
 * <pre>   {@code
 *   ToyContainer injector = new ToyContainer();
 *   // Bind basic dependencies
 *   injector.bind(Foo.class, new Foo(...));
 *   // Bind an interface to an implementation
 *   injector.bind(Fooable.class, Foo.class);
 *   // Get a value from the injector
 *   Foo foo = injector.resolve(Foo.class);
 * }</pre>
 *
 * <p>For more examples of usage, see the test cases in ToyContainerTest.
 */
public class ToyContainer {
  private static final Logger logger = Logger.getLogger(ToyContainer.class.getName());

  private final Map<Class<?>, Callable<?>> providers = new HashMap<>();
  
  /**
   * Binds the given type to the given instance.
   * 
   * <p>When a value of type T is requested, it will always be resolved to the instance value.
   */
  public <T> void bind(Class<T> type, final T instance) {
    providers.put(type, new Callable<T>() {
      @Override
      public T call() throws Exception {
        return instance;
      }
    });
  }
  
  /**
   * Binds the given abstract type to the given concrete type.
   * 
   * <p>When a value of abstractType is requested, it will be resolved by injecting a value of
   * concreteType.
   */
  public <T> void bind(Class<T> abstractType, final Class<? extends T> concreteType) {
    providers.put(abstractType, new Callable<T>() {
      @Override
      public T call() throws Exception {
        return resolve(concreteType);
      }
    });
  }
  
  /**
   * Gets an instance of the given type, with its dependencies injected.
   */
  public <T> T resolve(Class<T> type) {
    logger.info("Attempting to resolve " + type);
    Callable<?> provider = providers.get(type);
    // If we already have a provider registered for this type, use it!
    if (provider != null) {
      T result;
      try {
        // The provider may recursively call into resolve (see above), so we may get a chain
        // of nested exceptions thrown here.
        //
        // We use type.cast() here because a simple cast to (T) would be erased by the compiler
        // and would mean the cast would have no effect at runtime (which would also trigger an
        // unchecked cast warning).
        result = type.cast(provider.call());
      } catch (Exception ex) {
        throw new InjectionException("Error while trying to inject " + type, ex);
      }
      return result;
    }
    // OK, we didn't have a provider registered for this type. Let's look for a constructor we
    // can use instead.
    Constructor<?>[] constructors = type.getConstructors();
    // For now, just look for a single public constructor. A more robust solution might be to look
    // for a constructor annotated with @Inject.
    if (constructors.length != 1) {
      throw new InjectionException("Unable to find a valid constructor on " + type);
    }
    // This is an unchecked cast because the return value for Class.getConstructors() is an
    // array of Constructor<?> instead of Constructor<T>, for extra safety because array covariance
    // plus invariant generic types is pretty evil. However, since we haven't mutated the array
    // since we got it, this should always be safe.
    @SuppressWarnings("unchecked")
    Constructor<T> constructor = (Constructor<T>) constructors[0];
    // Find all the types of the constructor arguments. For now, assume that they are all actual
    // types (not generic or with extra annotations).
    Class<?>[] parameterTypes = constructor.getParameterTypes();
    // Build an array to hold the arguments we will pass into the constructor.
    Object[] constructorArgs = new Object[parameterTypes.length];
    for (int i = 0; i < parameterTypes.length; i++) {
      // Resolve each argument from the injector.
      constructorArgs[i] = resolve(parameterTypes[i]);
    }
    // And now use those arguments to construct a new instance, and return that.
    T result;
    try {
      result = constructor.newInstance(constructorArgs);
    } catch (Exception ex) {
      throw new InjectionException("Error invoking constructor of " + type, ex);
    }
    return result;
  }
  
  /**
   * Exception type thrown when injection fails.
   */
  public static class InjectionException extends RuntimeException {
    public InjectionException() {}
    
    public InjectionException(Throwable cause) {
      super(cause);
    }
    
    public InjectionException(String message) {
      super(message);
    }
    
    public InjectionException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}