import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Locale;

import javax.imageio.*;
import javax.swing.*;

public class AirPlay {
	public static final String DNSSD_TYPE = "_airplay._tcp.local.";
	
	public static final String NONE = "None";
	public static final String SLIDE_LEFT = "SlideLeft";
	public static final String SLIDE_RIGHT = "SlideRight";
	public static final String DISSOLVE = "Dissolve";
	public static final String USERNAME = "Airplay";
	public static final int PORT = 7000;
	public static final int APPLETV_WIDTH = 1920;
	public static final int APPLETV_HEIGHT = 1080;
	public static final float APPLETV_ASPECT = (float) APPLETV_WIDTH/APPLETV_HEIGHT;
	
	protected String hostname;
	protected String name;
	protected int port;
	protected PhotoThread photothread;
	protected String password;
	protected Map params;
	protected String authorization;
	protected Auth auth;
	protected int appletv_width = APPLETV_WIDTH;
	protected int appletv_height = APPLETV_HEIGHT;
	protected float appletv_aspect = APPLETV_ASPECT;
	
	//AirPlay class
	public AirPlay(Service service) {
		this(service.hostname,service.port,service.name);
	}
	public AirPlay(String hostname) {
		this(hostname,PORT);
	}
	public AirPlay(String hostname, int port) {
		this(hostname,port,hostname);
	}
	public AirPlay(String hostname, int port, String name) {
		this.hostname = hostname;
		this.port = port;
		this.name = name;
	}
	public void setScreenSize(int width, int height) {
		appletv_width = width;
		appletv_height = height;
		appletv_aspect = (float) width/height;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public void setAuth(Auth auth) {
		this.auth = auth;
	}
	protected String md5Digest(String input) {
		byte[] source;
		try {
			//Get byte according by specified coding.
			source = input.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			source = input.getBytes();
		}
		String result = null;
		char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7',
				'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(source);
			//The result should be one 128 integer
			byte temp[] = md.digest();
			char str[] = new char[16 * 2];
			int k = 0;
			for (int i = 0; i < 16; i++) {
				byte byte0 = temp[i];
				str[k++] = hexDigits[byte0 >>> 4 & 0xf];
				str[k++] = hexDigits[byte0 & 0xf];
			}
			result = new String(str);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;

	}
	protected String makeAuthorization(Map params, String password, String method, String uri) {
		String realm = (String) params.get("realm");
		String nonce = (String) params.get("nonce");
		String ha1 = md5Digest(USERNAME+":"+realm+":"+password);
		String ha2 = md5Digest(method+":"+uri);
		String response = md5Digest(ha1+":"+nonce+":"+ha2);
		authorization = "Digest username=\""+USERNAME+"\", "
			+"realm=\""+realm+"\", "
			+"nonce=\""+nonce+"\", "
			+"uri=\""+uri+"\", "
			+"response=\""+response+"\"";
		return authorization;
	}
	protected Map getAuthParams(String authString) {
		Map params = new HashMap();
		int firstSpace = authString.indexOf(' ');
		String digest = authString.substring(0,firstSpace);
		String rest = authString.substring(firstSpace+1).replaceAll("\r\n"," ");
		String[] lines = rest.split("\", ");
		for (int i = 0; i < lines.length; i++) {
			int split = lines[i].indexOf("=\"");
			String key = lines[i].substring(0,split);
			String value = lines[i].substring(split+2);
			if (value.charAt(value.length()-1) == '"') {
				value = value.substring(0,value.length()-1);
			}
			params.put(key,value);
		}
		return params;
	}
	protected String setPassword() throws IOException {
		if (password != null) {
			return password;
		} else {
			if (auth != null) {
				password = auth.getPassword(hostname,name);
				return password;
			} else {
				throw new IOException("Authorisation requied");
			}
		}
	}
	protected String doHTTP(String method, String uri) throws IOException {
		return doHTTP(method, uri, null);
	}
	protected String doHTTP(String method, String uri, ByteArrayOutputStream os) throws IOException {
		return doHTTP(method, uri, os, null);
	}
	protected String doHTTP(String method, String uri, ByteArrayOutputStream os, Map headers) throws IOException {
		return doHTTP(method, uri, os, new HashMap(), true);
	}
	protected String doHTTP(String method, String uri, ByteArrayOutputStream os, Map headers, boolean repeat) throws IOException {
		URL url = null;
		try {
			url = new URL("http://"+hostname+":"+port+uri);
		} catch(MalformedURLException e) { }
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setUseCaches(false);
		conn.setDoOutput(true);
		conn.setRequestMethod(method);

		if (params != null) {
			//Try to reuse password if already set
			headers.put("Authorization",makeAuthorization(params,password,method,uri));
		}
		if (headers.size() > 0) {
			conn.setRequestProperty("User-Agent","MediaControl/1.0");
			Object[] keys = headers.keySet().toArray();
			for (int i = 0; i < keys.length; i++) {
				conn.setRequestProperty((String) keys[i],(String) headers.get(keys[i]));
			}
		}
		
		if (os != null) {
			byte[] data = os.toByteArray();
			conn.setRequestProperty("Content-Length",""+data.length);
		}
		conn.connect();
		if (os != null) {
			os.writeTo(conn.getOutputStream());
			os.flush();
			os.close();
		}
		
		if (conn.getResponseCode() == 401) {
			if (repeat) {
				String authstring = conn.getHeaderFields().get("WWW-Authenticate").get(0);
				if (setPassword() != null) {
					params = getAuthParams(authstring);
					return doHTTP(method,uri,os,headers,false);
				} else {
					return null;
				}
			} else {
				throw new IOException("Incorrect password");
			}
		} else {
			//TODO: Only readback Content-Length? - right now not doing, seems to work different than PHP
			//conn.getHeaderField("Content-Length");
			InputStream is = conn.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer response = new StringBuffer(); 
			while((line = rd.readLine()) != null) {
				response.append(line);
				response.append("\r\n");
			}
			rd.close();
			return response.toString();
		}
	}
	public void stop() {
		try {
			stopPhotoThread();
			doHTTP("POST", "/stop");
			params = null;
		} catch (Exception e) { }
	}
	protected BufferedImage scaleImage(BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		if (width <= appletv_width && height <= appletv_height) {
			return image;
		} else {
			int scaledheight;
			int scaledwidth;
			float image_aspect = (float) width/height;
			if (image_aspect > appletv_aspect) {
				scaledheight = new Float(appletv_width / image_aspect).intValue();
				scaledwidth = appletv_width;
			} else {
				scaledheight = appletv_height;
				scaledwidth = new Float(appletv_height * image_aspect).intValue();
			}
			BufferedImage scaledimage = new BufferedImage(scaledwidth, scaledheight, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = scaledimage.createGraphics();
			g.drawImage(image, 0, 0, scaledwidth, scaledheight, null); 
			g.dispose();
			return scaledimage;
		}
	}
	public void photo(String filename) throws IOException {
		this.photo(filename,NONE);
	}
	public void photo(String filename, String transition) throws IOException {
		this.photo(new File(filename),transition);
	}
	public void photo(File imagefile) throws IOException {
		this.photo(imagefile,NONE);
	}
	public void photo(File imagefile, String transition) throws IOException {
		BufferedImage image = ImageIO.read(imagefile);
		photo(image,transition);
	}
	public void photo(BufferedImage image) throws IOException {
		this.photo(image,NONE);
	}
	public void photo(BufferedImage image, String transition) throws IOException {
		stopPhotoThread();
		BufferedImage scaledimage = scaleImage(image);
		photoRaw(scaledimage,transition);
		photothread = new PhotoThread(this,scaledimage,5000);
		photothread.start();
	}
	protected void photoRawCompress(BufferedImage image, String transition) throws IOException {
		BufferedImage scaledimage = scaleImage(image);
		photoRaw(scaledimage, transition);
	}
	protected void photoRaw(BufferedImage image, String transition) throws IOException {
		Map headers = new HashMap();
		headers.put("X-Apple-Transition",transition);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		boolean resultWrite = ImageIO.write(image, "jpg", os);
		/* TODO: Could adjust quality
		 * http://www.java.net/node/689678
		 * http://www.exampledepot.com/egs/javax.imageio/JpegWrite.html
		 * http://www.sussmanprejza.com/ar/card/DataUpload.java
		 */
		doHTTP("PUT", "/photo", os, headers);
	}
	public static BufferedImage captureScreen() throws AWTException {
		Toolkit tk = Toolkit.getDefaultToolkit();
		Dimension dim = tk.getScreenSize();
		Rectangle rect = new Rectangle(dim);
		Robot robot = new Robot();
		BufferedImage image = robot.createScreenCapture(rect);		
		return image;
	}
	
	public class PhotoThread extends Thread {
		private final AirPlay airplay;
		private BufferedImage image = null;
		private int timeout = 5000;
		
		public PhotoThread(AirPlay airplay) {
			this(airplay,null,1000);
		}
		public PhotoThread(AirPlay airplay, BufferedImage image, int timeout) {
			this.airplay = airplay;
			this.image = image;
			this.timeout = timeout;
		}
		public void run() {
			while (!Thread.interrupted()) {
				try {
					if (image == null) {
						BufferedImage frame = airplay.scaleImage(AirPlay.captureScreen());
						airplay.photoRawCompress(frame, NONE);
					} else {
						airplay.photoRaw(image,NONE);
						Thread.sleep(Math.round(0.9*timeout));
					}
				} catch (InterruptedException e) {
					break;
				} catch (Exception e) {
					e.printStackTrace();
					break;
				}
			}
		}
	}
	public void stopPhotoThread() {
		if (photothread != null) {
			photothread.interrupt();
			while (photothread.isAlive());
			photothread = null;
		}
	}
	public void desktop() throws AWTException, IOException {
		stopPhotoThread();
		photothread = new PhotoThread(this);
		photothread.start();
	}
	
	//Auth classes
	public static interface Auth {
		public abstract String getPassword(String hostname, String name);
	}
	public static class AuthDialog implements Auth {
		private Window parent;
		public AuthDialog(Window parent) {
			this.parent = parent;
		}
		public String getPassword(String hostname, String name) {
			final JPasswordField password = new JPasswordField();
			JOptionPane optionPane = new JOptionPane(password,JOptionPane.PLAIN_MESSAGE,JOptionPane.OK_CANCEL_OPTION);
			JDialog dialog = optionPane.createDialog(parent,"Password:");
			dialog.setLocationRelativeTo(parent);
			dialog.setVisible(true);
			int result = (Integer)optionPane.getValue();
			dialog.dispose();
			if(result == JOptionPane.OK_OPTION){
				return new String(password.getPassword());
			}
			return null;
		}
	}
	public static class AuthConsole implements Auth {
		public String getPassword(String hostname, String name) {
			String display = hostname == name ? hostname : name+" ("+hostname+")";
			return AirPlay.waitforuser("Please input password for "+display);
		}
	}
	
	//Bonjour classes
	public static class Service {
		public String name;
		public String hostname;
		public int port;
		
		public Service(String hostname) {
			this(hostname,PORT);
		}
		public Service(String hostname, int port) {
			this(hostname,port,hostname);
		}
		
		public Service(String hostname, int port, String name) {
			this.hostname = hostname;
			this.port = port;
			this.name = name;
		}
	}
	
	// Command line functions
	public static void usage() {
		System.out.println("commands: -s {stop} | -p file {photo} | -d {desktop} | -?");
		System.out.println("java -jar airplay.jar -h hostname[:port] [-a password] command");
	}
	public static String waitforuser() {
		return waitforuser("Press return to quit");
	}
	public static String waitforuser(String message) {
		System.out.println(message);
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String s = null;
		try {
			while ((s = in.readLine()) != null && !(s.length() >= 0)) { }
		} catch (Exception e) {
			e.printStackTrace();
		}
		return s;
	}
	public static void main(String[] args) {
		try {
			CmdLineParser cmd = new CmdLineParser();
			CmdLineParser.Option hostopt = cmd.addStringOption('h',"hostname");
			CmdLineParser.Option stopopt = cmd.addBooleanOption('s',"stop");
			CmdLineParser.Option photoopt = cmd.addStringOption('p',"photo");
			CmdLineParser.Option desktopopt = cmd.addBooleanOption('d',"desktop");
			CmdLineParser.Option passopt = cmd.addStringOption('a',"password");
			CmdLineParser.Option helpopt = cmd.addBooleanOption('?',"help");
			cmd.parse(args);
			
			String hostname = (String) cmd.getOptionValue(hostopt);
			
			Boolean showHelp = (Boolean) cmd.getOptionValue(helpopt);
			
			AirPlay airplay;
			String[] hostport = hostname.split(":",2);
			if (hostport.length > 1) {
				airplay = new AirPlay(hostport[0],Integer.parseInt(hostport[1]));
			} else {
				airplay = new AirPlay(hostport[0]);
			}
			airplay.setAuth(new AuthConsole());
			String password = (String) cmd.getOptionValue(passopt);
			airplay.setPassword(password);
			String photo;
			if (cmd.getOptionValue(stopopt) != null) {
				airplay.stop();
			} else if ((photo = (String) cmd.getOptionValue(photoopt)) != null) {
				System.out.println("Press ctrl-c to quit");
				airplay.photo(photo);
			} else if (cmd.getOptionValue(desktopopt) != null) {
				System.out.println("Press ctrl-c to quit");
				airplay.desktop();
			} else {
				usage();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class CmdLineParser {

    /**
     * Base class for exceptions that may be thrown when options are parsed
     */
    public static abstract class OptionException extends Exception {
        OptionException(String msg) { super(msg); }
    }

    /**
     * Thrown when the parsed command-line contains an option that is not
     * recognised. <code>getMessage()</code> returns
     * an error string suitable for reporting the error to the user (in
     * English).
     */
    public static class UnknownOptionException extends OptionException {
        UnknownOptionException( String optionName ) {
            this(optionName, "Unknown option '" + optionName + "'");
        }

        UnknownOptionException( String optionName, String msg ) {
            super(msg);
            this.optionName = optionName;
        }

        /**
         * @return the name of the option that was unknown (e.g. "-u")
         */
        public String getOptionName() { return this.optionName; }
        private String optionName = null;
    }

    /**
     * Thrown when the parsed commandline contains multiple concatenated
     * short options, such as -abcd, where one is unknown.
     * <code>getMessage()</code> returns an english human-readable error
     * string.
     * @author Vidar Holen
     */
    public static class UnknownSuboptionException
        extends UnknownOptionException {
        private char suboption;

        UnknownSuboptionException( String option, char suboption ) {
            super(option, "Illegal option: '"+suboption+"' in '"+option+"'");
            this.suboption=suboption;
        }
        public char getSuboption() { return suboption; }
    }

    /**
     * Thrown when the parsed commandline contains multiple concatenated
     * short options, such as -abcd, where one or more requires a value.
     * <code>getMessage()</code> returns an english human-readable error
     * string.
     * @author Vidar Holen
     */
    public static class NotFlagException extends UnknownOptionException {
        private char notflag;

        NotFlagException( String option, char unflaggish ) {
            super(option, "Illegal option: '"+option+"', '"+
                  unflaggish+"' requires a value");
            notflag=unflaggish;
        }

        /**
         * @return the first character which wasn't a boolean (e.g 'c')
         */
        public char getOptionChar() { return notflag; }
    }

    /**
     * Thrown when an illegal or missing value is given by the user for
     * an option that takes a value. <code>getMessage()</code> returns
     * an error string suitable for reporting the error to the user (in
     * English).
     */
    public static class IllegalOptionValueException extends OptionException {
        public IllegalOptionValueException( Option opt, String value ) {
            super("Illegal value '" + value + "' for option " +
                  (opt.shortForm() != null ? "-" + opt.shortForm() + "/" : "") +
                  "--" + opt.longForm());
            this.option = opt;
            this.value = value;
        }

        /**
         * @return the name of the option whose value was illegal (e.g. "-u")
         */
        public Option getOption() { return this.option; }

        /**
         * @return the illegal value
         */
        public String getValue() { return this.value; }
        private Option option;
        private String value;
    }

    /**
     * Representation of a command-line option
     */
    public static abstract class Option {

        protected Option( String longForm, boolean wantsValue ) {
            this(null, longForm, wantsValue);
        }

        protected Option( char shortForm, String longForm,
                          boolean wantsValue ) {
            this(new String(new char[]{shortForm}), longForm, wantsValue);
        }

        private Option( String shortForm, String longForm, boolean wantsValue ) {
            if ( longForm == null )
                throw new IllegalArgumentException("Null longForm not allowed");
            this.shortForm = shortForm;
            this.longForm = longForm;
            this.wantsValue = wantsValue;
        }

        public String shortForm() { return this.shortForm; }

        public String longForm() { return this.longForm; }

        /**
         * Tells whether or not this option wants a value
         */
        public boolean wantsValue() { return this.wantsValue; }

        public final Object getValue( String arg, Locale locale )
            throws IllegalOptionValueException {
            if ( this.wantsValue ) {
                if ( arg == null ) {
                    throw new IllegalOptionValueException(this, "");
                }
                return this.parseValue(arg, locale);
            }
            else {
                return Boolean.TRUE;
            }
        }

        /**
         * Override to extract and convert an option value passed on the
         * command-line
         */
        protected Object parseValue( String arg, Locale locale )
            throws IllegalOptionValueException {
            return null;
        }

        private String shortForm = null;
        private String longForm = null;
        private boolean wantsValue = false;

        public static class BooleanOption extends Option {
            public BooleanOption( char shortForm, String longForm ) {
                super(shortForm, longForm, false);
            }
            public BooleanOption( String longForm ) {
                super(longForm, false);
            }
        }

        /**
         * An option that expects an integer value
         */
        public static class IntegerOption extends Option {
            public IntegerOption( char shortForm, String longForm ) {
                super(shortForm, longForm, true);
            }
            public IntegerOption( String longForm ) {
                super(longForm, true);
            }
            protected Object parseValue( String arg, Locale locale )
                throws IllegalOptionValueException {
                try {
                    return new Integer(arg);
                }
                catch (NumberFormatException e) {
                    throw new IllegalOptionValueException(this, arg);
                }
            }
        }

        /**
         * An option that expects a long integer value
         */
        public static class LongOption extends Option {
            public LongOption( char shortForm, String longForm ) {
                super(shortForm, longForm, true);
            }
            public LongOption( String longForm ) {
                super(longForm, true);
            }
            protected Object parseValue( String arg, Locale locale )
                throws IllegalOptionValueException {
                try {
                    return new Long(arg);
                }
                catch (NumberFormatException e) {
                    throw new IllegalOptionValueException(this, arg);
                }
            }
        }

        /**
         * An option that expects a floating-point value
         */
        public static class DoubleOption extends Option {
            public DoubleOption( char shortForm, String longForm ) {
                super(shortForm, longForm, true);
            }
            public DoubleOption( String longForm ) {
                super(longForm, true);
            }
            protected Object parseValue( String arg, Locale locale )
                throws IllegalOptionValueException {
                try {
                    NumberFormat format = NumberFormat.getNumberInstance(locale);
                    Number num = (Number)format.parse(arg);
                    return new Double(num.doubleValue());
                }
                catch (ParseException e) {
                    throw new IllegalOptionValueException(this, arg);
                }
            }
        }

        /**
         * An option that expects a string value
         */
        public static class StringOption extends Option {
            public StringOption( char shortForm, String longForm ) {
                super(shortForm, longForm, true);
            }
            public StringOption( String longForm ) {
                super(longForm, true);
            }
            protected Object parseValue( String arg, Locale locale ) {
                return arg;
            }
        }
    }

    /**
     * Add the specified Option to the list of accepted options
     */
    public final Option addOption( Option opt ) {
        if ( opt.shortForm() != null )
            this.options.put("-" + opt.shortForm(), opt);
        this.options.put("--" + opt.longForm(), opt);
        return opt;
    }

    /**
     * Convenience method for adding a string option.
     * @return the new Option
     */
    public final Option addStringOption( char shortForm, String longForm ) {
        return addOption(new Option.StringOption(shortForm, longForm));
    }

    /**
     * Convenience method for adding a string option.
     * @return the new Option
     */
    public final Option addStringOption( String longForm ) {
        return addOption(new Option.StringOption(longForm));
    }

    /**
     * Convenience method for adding an integer option.
     * @return the new Option
     */
    public final Option addIntegerOption( char shortForm, String longForm ) {
        return addOption(new Option.IntegerOption(shortForm, longForm));
    }

    /**
     * Convenience method for adding an integer option.
     * @return the new Option
     */
    public final Option addIntegerOption( String longForm ) {
        return addOption(new Option.IntegerOption(longForm));
    }

    /**
     * Convenience method for adding a long integer option.
     * @return the new Option
     */
    public final Option addLongOption( char shortForm, String longForm ) {
        return addOption(new Option.LongOption(shortForm, longForm));
    }

    /**
     * Convenience method for adding a long integer option.
     * @return the new Option
     */
    public final Option addLongOption( String longForm ) {
        return addOption(new Option.LongOption(longForm));
    }

    /**
     * Convenience method for adding a double option.
     * @return the new Option
     */
    public final Option addDoubleOption( char shortForm, String longForm ) {
        return addOption(new Option.DoubleOption(shortForm, longForm));
    }

    /**
     * Convenience method for adding a double option.
     * @return the new Option
     */
    public final Option addDoubleOption( String longForm ) {
        return addOption(new Option.DoubleOption(longForm));
    }

    /**
     * Convenience method for adding a boolean option.
     * @return the new Option
     */
    public final Option addBooleanOption( char shortForm, String longForm ) {
        return addOption(new Option.BooleanOption(shortForm, longForm));
    }

    /**
     * Convenience method for adding a boolean option.
     * @return the new Option
     */
    public final Option addBooleanOption( String longForm ) {
        return addOption(new Option.BooleanOption(longForm));
    }

    /**
     * Equivalent to {@link #getOptionValue(Option, Object) getOptionValue(o,
     * null)}.
     */
    public final Object getOptionValue( Option o ) {
        return getOptionValue(o, null);
    }


    /**
     * @return the parsed value of the given Option, or the given default 'def'
     * if the option was not set
     */
    public final Object getOptionValue( Option o, Object def ) {
        Vector v = (Vector)values.get(o.longForm());

        if (v == null) {
            return def;
        }
        else if (v.isEmpty()) {
            return null;
        }
        else {
            Object result = v.elementAt(0);
            v.removeElementAt(0);
            return result;
        }
    }


    /**
     * @return A Vector giving the parsed values of all the occurrences of the
     * given Option, or an empty Vector if the option was not set.
     */
    public final Vector getOptionValues( Option option ) {
        Vector result = new Vector();

        while (true) {
            Object o = getOptionValue(option, null);

            if (o == null) {
                return result;
            }
            else {
                result.addElement(o);
            }
        }
    }


    /**
     * @return the non-option arguments
     */
    public final String[] getRemainingArgs() {
        return this.remainingArgs;
    }

    /**
     * Extract the options and non-option arguments from the given
     * list of command-line arguments. The default locale is used for
     * parsing options whose values might be locale-specific.
     */
    public final void parse( String[] argv )
        throws IllegalOptionValueException, UnknownOptionException {

        // It would be best if this method only threw OptionException, but for
        // backwards compatibility with old user code we throw the two
        // exceptions above instead.

        parse(argv, Locale.getDefault());
    }

    /**
     * Extract the options and non-option arguments from the given
     * list of command-line arguments. The specified locale is used for
     * parsing options whose values might be locale-specific.
     */
    public final void parse( String[] argv, Locale locale )
        throws IllegalOptionValueException, UnknownOptionException {

        // It would be best if this method only threw OptionException, but for
        // backwards compatibility with old user code we throw the two
        // exceptions above instead.

        Vector otherArgs = new Vector();
        int position = 0;
        this.values = new Hashtable(10);
        while ( position < argv.length ) {
            String curArg = argv[position];
            if ( curArg.startsWith("-") ) {
                if ( curArg.equals("--") ) { // end of options
                    position += 1;
                    break;
                }
                String valueArg = null;
                if ( curArg.startsWith("--") ) { // handle --arg=value
                    int equalsPos = curArg.indexOf("=");
                    if ( equalsPos != -1 ) {
                        valueArg = curArg.substring(equalsPos+1);
                        curArg = curArg.substring(0,equalsPos);
                    }
                } else if(curArg.length() > 2) {  // handle -abcd
                    for(int i=1; i<curArg.length(); i++) {
                        Option opt=(Option)this.options.get
                            ("-"+curArg.charAt(i));
                        if(opt==null) throw new 
                            UnknownSuboptionException(curArg,curArg.charAt(i));
                        if(opt.wantsValue()) throw new
                            NotFlagException(curArg,curArg.charAt(i));
                        addValue(opt, opt.getValue(null,locale));
                        
                    }
                    position++;
                    continue;
                }
                
                Option opt = (Option)this.options.get(curArg);
                if ( opt == null ) {
                    throw new UnknownOptionException(curArg);
                }
                Object value = null;
                if ( opt.wantsValue() ) {
                    if ( valueArg == null ) {
                        position += 1;
                        if ( position < argv.length ) {
                            valueArg = argv[position];
                        }
                    }
                    value = opt.getValue(valueArg, locale);
                }
                else {
                    value = opt.getValue(null, locale);
                }

                addValue(opt, value);

                position += 1;
            }
            else {
                otherArgs.addElement(curArg);
                position += 1;
            }
        }
        for ( ; position < argv.length; ++position ) {
            otherArgs.addElement(argv[position]);
        }

        this.remainingArgs = new String[otherArgs.size()];
        otherArgs.copyInto(remainingArgs);
    }


    private void addValue(Option opt, Object value) {
        String lf = opt.longForm();

        Vector v = (Vector)values.get(lf);

        if (v == null) {
            v = new Vector();
            values.put(lf, v);
        }

        v.addElement(value);
    }


    private String[] remainingArgs = null;
    private Hashtable options = new Hashtable(10);
    private Hashtable values = new Hashtable(10);
}