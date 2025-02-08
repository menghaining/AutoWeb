package ict.pag.m.instrumentation.Util;

/** all information need to be configured */
public class ConfigureUtil {

	/**
	 * user configured </br>
	 * like com/example/class
	 */
	public static boolean isApplicationClass(String className) {

		/* JackEE runnable start */
		// shopizer
		if (className.startsWith("com/salesmanager"))
			return true;
		// springblog
		if (className.startsWith("com/raysmond/blog"))
			return true;
		// WebGoat
		if (className.startsWith("org/owasp/webgoat"))
			return true;
		// pybbs
		if (className.startsWith("co/yiiu/pybbs"))
			return true;
//		// opencms
//		if(className.startsWith("org/opencms"))
//			return true;
		/* JackEE runnable end */

//		if (className.startsWith("testProj/test/instrumental"))
//			return true;
//		if (className.startsWith("testInstrumenrtal"))
//			return true;
//		/**
//		 * for OWASP
//		 */
//		if (className.startsWith("org/owasp/benchmark"))
//			return true;
//
		if (className.startsWith("com/example"))
			return true;
		/**
		 * for spring-pet-clinic
		 * 
		 */
		if (className.startsWith("org/springframework/samples/"))
			return true;
		/**
		 * for spring-mvc-showcase
		 * 
		 */
		if (className.startsWith("org/springframework/samples/mvc/"))
			return true;
		/**
		 * for opencms
		 */
//		if (className.startsWith("org/opencms"))
//			return true;
		/**
		 * for WebGoat
		 */
//		if (className.startsWith("org/owasp/webgoat"))
//			return true;
		/**
		 * for Shopizer
		 */
//		if (className.startsWith("com/salesmanager"))
//			return true;
		/**
		 * for pybbs
		 */
//		if (className.startsWith("co/yiiu/pybbs"))
//			return true;
		/**
		 * for SpringBlog
		 */
//		if (className.startsWith("com/raysmond"))
//			return true;

		/**
		 * for Openmrs</br>
		 * jdk bug report</br>
		 * drop
		 */
//		if (className.startsWith("org/openmrs"))
//			return true;

		/** for iCloud */
		if (className.startsWith("cn/zju/"))
			return true;

		/** apache-struts2-examples */
		if (className.startsWith("example/") || className.startsWith("org/demo/") || className.startsWith("org/apache/struts/edit/")
				|| className.startsWith("org/apache/examples/struts") || className.startsWith("org/apache/struts_example")
				|| className.startsWith("org/apache/struts/using_tags/helloworld/") || className.startsWith("org/apache/struts/helloworld/")
				|| className.startsWith("org/apache/struts/register/") || className.startsWith("org/apache/struts/example/")
				|| className.startsWith("org/apache/struts/examples/") || className.startsWith("org/apache/struts_examples/")
				|| className.startsWith("org/apache/strutsexamples/") || className.startsWith("org/apache/strutsexamples/")
				|| className.startsWith("org/apache/struts/using_tags/") || className.startsWith("org/apache/struts/crud/")
				|| className.startsWith("org/apache/struts2/portlet/example/") || className.startsWith("org/apache/struts2/shiro/example/")
				|| className.startsWith("org/apache/struts/validation_messages/") || className.startsWith("org/apache/struts/tutorials/wildcardmethod/")
				|| className.startsWith("org/apache/struts/tutorials/wildcardmethod/") || className.startsWith("org/apache/struts/form"))
			return true;

		/** openmk */
		if (className.startsWith("com/openkm/"))
			return true;

//
		/** hello-web */
//		if (className.startsWith("acme/hello/"))
//			return true;

		/** jpetstore */
		if (className.startsWith("org/mybatis/jpetstore/"))
			return true;

		/** Ruoyi */
		if (className.startsWith("com/ruoyi/"))
			return true;

		/** sell.jar */
		if (className.startsWith("com/imooc/"))
			return true;

		/** halo blog */
		if (className.startsWith("run/halo/app/"))
			return true;

		/** com.mycollab. */
//		if(className.startsWith("com/mycollab/"))
//			return true;

		/** LogisticsManageSystem */
		if (className.startsWith("com/wt/"))
			return true;

		/** NewsSystem */
		if (className.startsWith("magicgis/newssystem/"))
			return true;

		/** newbee-mall system */
		if (className.startsWith("ltd/newbee/mall/"))
			return true;

		/** life.majiang.community */
		if (className.startsWith("life/majiang/community/"))
			return true;

		/** logical-dms: com.logicaldoc. */
		if (className.startsWith("com/logicaldoc/"))
			return true;

		/** struts2-vuln-samples */
		if (className.startsWith("org/test/") || className.startsWith("org/demo/rest/example"))
			return true;

		/** mission-shop */
		if (className.startsWith("com/cn/shop/"))
			return true;

		/** webprotege */
//		if (className.startsWith("edu/stanford/bmir/protege/web/"))
//			return true;

		return false;
	}

	public static boolean isExclude(String className) {
		if (className.startsWith("org/openmrs/standalone"))
			return true;

		return false;
	}

	public static boolean isCommonJavaClass(String className) {
		if (className.startsWith("java/util"))
			return true;
		if (className.startsWith("java/lang"))
			return true;
		return false;
	}

	/** user-configured concerned framework class */
	public static boolean isFrameworkMarks(String name) {
		if (name.startsWith("L") && name.contains("/"))
			name = name.substring(1).replace('/', '.');

		if (name.startsWith("java.lang") || name.startsWith("java.util") || name.startsWith("java.io") || name.startsWith("javax.validation"))
			return false;

		return true;
	}

}
