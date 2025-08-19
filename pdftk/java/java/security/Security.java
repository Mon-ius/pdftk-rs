package java.security;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.security.Provider;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
public final class Security extends Object
{
  private static final String ALG_ALIAS = "Alg.Alias.";
  private static Vector providers = new Vector();
  private static Properties secprops = new Properties();
  static
    {
	providers.addElement (new gnu.java.security.provider.Gnu());
  }
  private Security()
  {
  }
  private static boolean loadProviders(String baseUrl, String vendor)
  {
    if (baseUrl == null || vendor == null)
      return false;
    boolean result = true;
    String secfilestr = baseUrl + "/security/" + vendor + ".security";
    try
      {
	InputStream fin = new URL(secfilestr).openStream();
	secprops.load(fin);
	int i = 1;
	String name;
	while ((name = secprops.getProperty("security.provider." + i)) != null)
	  {
	    Exception exception = null;
	    try
	      {
		providers.addElement(Class.forName(name).newInstance());
	      }
	    catch (ClassNotFoundException x)
	      {
	        exception = x;
	      }
	    catch (InstantiationException x)
	      {
	        exception = x;
	      }
	    catch (IllegalAccessException x)
	      {
	        exception = x;
	      }
	    if (exception != null)
	      {
		System.err.println ("WARNING: Error loading security provider "
				    + name + ": " + exception);
		result = false;
	      }
	    i++;
	  }
      }
    catch (IOException ignored)
      {
	result = false;
      }
    return false;
  }
  public static String getAlgorithmProperty(String algName, String propName)
  {
    if (algName == null || propName == null)
      return null;
    String property = String.valueOf(propName) + "." + String.valueOf(algName);
    Provider p;
    for (Iterator i = providers.iterator(); i.hasNext(); )
      {
        p = (Provider) i.next();
        for (Iterator j = p.keySet().iterator(); j.hasNext(); )
          {
            String key = (String) j.next();
            if (key.equalsIgnoreCase(property))
              return p.getProperty(key);
          }
      }
    return null;
  }
  public static int insertProviderAt(Provider provider, int position)
  {
    SecurityManager sm = System.getSecurityManager();
    if (sm != null)
      sm.checkSecurityAccess("insertProvider." + provider.getName());
    position--;
    int max = providers.size ();
    for (int i = 0; i < max; i++)
      {
	if (((Provider) providers.elementAt(i)).getName() == provider.getName())
	  return -1;
      }
    if (position < 0)
      position = 0;
    if (position > max)
      position = max;
    providers.insertElementAt(provider, position);
    return position + 1;
  }
  public static int addProvider(Provider provider)
  {
    return insertProviderAt (provider, providers.size () + 1);
  }
  public static void removeProvider(String name)
  {
    SecurityManager sm = System.getSecurityManager();
    if (sm != null)
      sm.checkSecurityAccess("removeProvider." + name);
    int max = providers.size ();
    for (int i = 0; i < max; i++)
      {
	if (((Provider) providers.elementAt(i)).getName() == name)
	  {
	    providers.remove(i);
	    break;
	  }
      }
  }
  public static Provider[] getProviders()
  {
    Provider array[] = new Provider[providers.size ()];
    providers.copyInto (array);
    return array;
  }
  public static Provider getProvider(String name)
  {
    Provider p;
    int max = providers.size ();
    for (int i = 0; i < max; i++)
      {
	p = (Provider) providers.elementAt(i);
	if (p.getName() == name)
	  return p;
      }
    return null;
  }
  public static String getProperty(String key)
  {
    SecurityManager sm = System.getSecurityManager();
    if (sm != null)
      sm.checkSecurityAccess("getProperty." + key);
    return secprops.getProperty(key);
  }
  public static void setProperty(String key, String datnum)
  {
    SecurityManager sm = System.getSecurityManager();
    if (sm != null)
      sm.checkSecurityAccess("setProperty." + key);
    secprops.put(key, datnum);
  }
  public static Set getAlgorithms(String serviceName)
  {
    HashSet result = new HashSet();
    if (serviceName == null || serviceName.length() == 0)
      return result;
    serviceName = serviceName.trim();
    if (serviceName.length() == 0)
      return result;
    serviceName = serviceName.toUpperCase()+".";
    Provider[] providers = getProviders();
    int ndx;
    for (int i = 0; i < providers.length; i++)
      for (Enumeration e = providers[i].propertyNames(); e.hasMoreElements(); )
        {
          String service = ((String) e.nextElement()).trim();
          if (service.toUpperCase().startsWith(serviceName))
            {
              service = service.substring(serviceName.length()).trim();
              ndx = service.indexOf(' ');
              if (ndx != -1)
                service = service.substring(0, ndx);
              result.add(service);
            }
        }
    return Collections.unmodifiableSet(result);
  }
  public static Provider[] getProviders(String filter)
  {
    if (providers == null || providers.isEmpty())
      return null;
    if (filter == null || filter.length() == 0)
      return getProviders();
    HashMap map = new HashMap(1);
    int i = filter.indexOf(':');
    if (i == -1)
      map.put(filter, "");
    else
      map.put(filter.substring(0, i), filter.substring(i+1));
    return getProviders(map);
  }
  public static Provider[] getProviders(Map filter)
  {
    if (providers == null || providers.isEmpty())
      return null;
    if (filter == null)
      return getProviders();
    Set querries = filter.keySet();
    if (querries == null || querries.isEmpty())
      return getProviders();
    LinkedHashSet result = new LinkedHashSet(providers);
    int dot, ws;
    String querry, service, algorithm, attribute, value;
    LinkedHashSet serviceProviders = new LinkedHashSet();
    for (Iterator i = querries.iterator(); i.hasNext(); )
      {
        querry = (String) i.next();
        if (querry == null)
          continue;
        querry = querry.trim();
        if (querry.length() == 0)
          continue;
        dot = querry.indexOf('.');
        if (dot == -1)
          throw new InvalidParameterException(
              "missing dot in '" + String.valueOf(querry)+"'");
        value = (String) filter.get(querry);
        if (value == null || value.trim().length() == 0)
          {
            value = null;
            attribute = null;
            service = querry.substring(0, dot).trim();
            algorithm = querry.substring(dot+1).trim();
          }
        else
          {
            ws = querry.indexOf(' ');
            if (ws == -1)
              throw new InvalidParameterException(
                  "value (" + String.valueOf(value) +
                  ") is not empty, but querry (" + String.valueOf(querry) +
                  ") is missing at least one space character");
            value = value.trim();
            attribute = querry.substring(ws+1).trim();
            if (attribute.indexOf('.') != -1)
              throw new InvalidParameterException(
                  "attribute_name (" + String.valueOf(attribute) +
                  ") in querry (" + String.valueOf(querry) + ") contains a dot");
            querry = querry.substring(0, ws).trim();
            service = querry.substring(0, dot).trim();
            algorithm = querry.substring(dot+1).trim();
          }
        if (service.length() == 0)
          throw new InvalidParameterException(
              "<crypto_service> in querry (" + String.valueOf(querry) +
              ") is empty");
        if (algorithm.length() == 0)
          throw new InvalidParameterException(
              "<algorithm_or_type> in querry (" + String.valueOf(querry) +
              ") is empty");
        selectProviders(service, algorithm, attribute, value, result, serviceProviders);
        result.retainAll(serviceProviders);
        if (result.isEmpty())
          break;
      }
    if (result.isEmpty())
      return null;
    return (Provider[]) result.toArray(new Provider[0]);
  }
  private static void selectProviders(String svc, String algo, String attr,
                                      String val, LinkedHashSet providerSet,
                                      LinkedHashSet result)
  {
    result.clear();
    for (Iterator i = providerSet.iterator(); i.hasNext(); )
      {
        Provider p = (Provider) i.next();
        if (provides(p, svc, algo, attr, val))
          result.add(p);
      }
  }
  private static boolean provides(Provider p, String svc, String algo,
                                  String attr, String val)
  {
    Iterator it;
    String serviceDotAlgorithm = null;
    String key = null;
    String realVal;
    boolean found = false;
    outer: for (int r = 0; r < 3; r++)
      {
        serviceDotAlgorithm = (svc+"."+String.valueOf(algo)).trim();
        inner: for (it = p.keySet().iterator(); it.hasNext(); )
          {
            key = (String) it.next();
            if (key.equalsIgnoreCase(serviceDotAlgorithm))
              {
                found = true;
                break outer;
              }
            if (key.equalsIgnoreCase(ALG_ALIAS + serviceDotAlgorithm))
              {
                algo = p.getProperty(key);
                continue outer;
              }
          }
      }
    if (!found)
      return false;
    if (val == null)
      return true;
    String realAttr;
    int limit = serviceDotAlgorithm.length() + 1;
    for (it = p.keySet().iterator(); it.hasNext(); )
      {
        key = (String) it.next();
        if (key.length() <= limit)
          continue;
        if (key.substring(0, limit).equalsIgnoreCase(serviceDotAlgorithm+" "))
          {
            realAttr = key.substring(limit).trim();
            if (! realAttr.equalsIgnoreCase(attr))
              continue;
            realVal = p.getProperty(key);
            if (realVal == null)
              return false;
            realVal = realVal.trim();
            if (val.equalsIgnoreCase(realVal))
              return true;
            return (new Integer(val).intValue() >= new Integer(realVal).intValue());
          }
      }
    return false;
  }
}