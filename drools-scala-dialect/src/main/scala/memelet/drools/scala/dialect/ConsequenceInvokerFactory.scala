package memelet.drools.scala.dialect


//class ConsequenceInvokerFactory
//
//import com.espertech.esper.client.EPSubscriberException
//import com.espertech.esper.util.JavaClassHelper
//import java.lang.reflect.Method
//import java.lang.reflect.Modifier
//import java.util.ArrayList
//import java.util.Map
//
//
//object ConsequenceInvokerFactory {
//  /**
//   * Creates a strategy implementation that indicates to subscribers
//   * the statement results based on the select-clause columns.
//   * @param subscriber to indicate to
//   * @param selectClauseTypes are the types of each column in the select clause
//   * @param selectClauseColumns the names of each column in the select clause
//   * @return strategy for dispatching naturals
//   * @throws EPSubscriberException if the subscriber is invalid
//   */
//  def create(subscriber: Any, selectClauseTypes: Array[Class[_]], selectClauseColumns: Array[String]): ResultDeliveryStrategy = {
//    var subscriptionMethod: Method = null
//    var updateMethods: ArrayList[Method] = new ArrayList[Method]
//    for (method <- subscriber.getClass.getMethods) {
//      if ((method.getName.equals("update")) && (Modifier.isPublic(method.getModifiers))) {
//        updateMethods.add(method)
//      }
//    }
//    if (updateMethods.size == 0) {
//      var message: String = "Subscriber object does not provide a public method by name 'update'"
//      throw new EPSubscriberException(message)
//    }
//    var isMapArrayDelivery: Boolean = false
//    var isObjectArrayDelivery: Boolean = false
//    var isSingleRowMap: Boolean = false
//    var isSingleRowObjectArr: Boolean = false
//    var isTypeArrayDelivery: Boolean = false
//    for (method <- updateMethods) {
//      var parameters: Array[Class[_]] = method.getParameterTypes
//      if (parameters.length == selectClauseTypes.length) {
//        var fitsParameters: Boolean = true
//
//        {
//          var i: Int = 0
//          while (i < parameters.length) {
//            {
//              var boxedExpressionType: Class[_] = JavaClassHelper.getBoxedType(selectClauseTypes(i))
//              var boxedParameterType: Class[_] = JavaClassHelper.getBoxedType(parameters(i))
//              if ((boxedExpressionType != null) && (!JavaClassHelper.isAssignmentCompatible(boxedExpressionType, boxedParameterType))) {
//                fitsParameters = false
//                break //todo: break is not supported
//              }
//            }
//            ({i += 1; i})
//          }
//        }
//        if (fitsParameters) {
//          subscriptionMethod = method
//          break //todo: break is not supported
//        }
//      }
//      if ((parameters.length == 1) && (parameters(0) == classOf[Map[_, _]])) {
//        isSingleRowMap = true
//        subscriptionMethod = method
//        break //todo: break is not supported
//      }
//      if ((parameters.length == 1) && (parameters(0) == classOf[Array[Any]])) {
//        isSingleRowObjectArr = true
//        subscriptionMethod = method
//        break //todo: break is not supported
//      }
//      if ((parameters.length == 2) && (parameters(0) == classOf[Array[Map[_, _]]]) && (parameters(1) == classOf[Array[Map[_, _]]])) {
//        subscriptionMethod = method
//        isMapArrayDelivery = true
//        break //todo: break is not supported
//      }
//      if ((parameters.length == 2) && (parameters(0) == classOf[Array[Array[Any]]]) && (parameters(1) == classOf[Array[Array[Any]]])) {
//        subscriptionMethod = method
//        isObjectArrayDelivery = true
//        break //todo: break is not supported
//      }
//      if ((parameters.length == 2) && (parameters(0).equals(parameters(1))) && (parameters(0).isArray) && (selectClauseTypes.length == 1)) {
//        var componentType: Class[_] = parameters(0).getComponentType
//        if (JavaClassHelper.isAssignmentCompatible(selectClauseTypes(0), componentType)) {
//          subscriptionMethod = method
//          isTypeArrayDelivery = true
//          break //todo: break is not supported
//        }
//      }
//    }
//    if (subscriptionMethod == null) {
//      if (updateMethods.size > 1) {
//        var parametersDesc: String = JavaClassHelper.getParameterAsString(selectClauseTypes)
//        var message: String = "No suitable subscriber method named 'update' found, expecting a method that takes " + selectClauseTypes.length + " parameter of type " + parametersDesc
//        throw new EPSubscriberException(message)
//      }
//      else {
//        var parameters: Array[Class[_]] = updateMethods.get(0).getParameterTypes
//        var parametersDesc: String = JavaClassHelper.getParameterAsString(selectClauseTypes)
//        if (parameters.length != selectClauseTypes.length) {
//          var message: String = "No suitable subscriber method named 'update' found, expecting a method that takes " + selectClauseTypes.length + " parameter of type " + parametersDesc
//          throw new EPSubscriberException(message)
//        }
//
//        {
//          var i: Int = 0
//          while (i < parameters.length) {
//            {
//              var boxedExpressionType: Class[_] = JavaClassHelper.getBoxedType(selectClauseTypes(i))
//              var boxedParameterType: Class[_] = JavaClassHelper.getBoxedType(parameters(i))
//              if ((boxedExpressionType != null) && (!JavaClassHelper.isAssignmentCompatible(boxedExpressionType, boxedParameterType))) {
//                var message: String = "Subscriber method named 'update' for parameter number " + (i + 1) + " is not assignable, " + "expecting type '" + JavaClassHelper.getParameterAsString(selectClauseTypes(i)) + "' but found type '" + JavaClassHelper.getParameterAsString(parameters(i)) + "'"
//                throw new EPSubscriberException(message)
//              }
//            }
//            ({i += 1; i})
//          }
//        }
//      }
//    }
//    if (isMapArrayDelivery) {
//      return new ResultDeliveryStrategyMap(subscriber, subscriptionMethod, selectClauseColumns)
//    }
//    else if (isObjectArrayDelivery) {
//      return new ResultDeliveryStrategyObjectArr(subscriber, subscriptionMethod)
//    }
//    else if (isTypeArrayDelivery) {
//      return new ResultDeliveryStrategyTypeArr(subscriber, subscriptionMethod)
//    }
//    var startMethod: Method = null
//    var endMethod: Method = null
//    var rStreamMethod: Method = null
//    try {
//      startMethod = subscriber.getClass.getMethod("updateStart", classOf[Int], classOf[Int])
//    }
//    catch {
//      case e: NoSuchMethodException => {
//      }
//    }
//    try {
//      endMethod = subscriber.getClass.getMethod("updateEnd")
//    }
//    catch {
//      case e: NoSuchMethodException => {
//      }
//    }
//    try {
//      rStreamMethod = subscriber.getClass.getMethod("updateRStream", subscriptionMethod.getParameterTypes)
//    }
//    catch {
//      case e: NoSuchMethodException => {
//      }
//    }
//    var convertor: DeliveryConvertor = null
//    if (isSingleRowMap) {
//      convertor = new DeliveryConvertorMap(selectClauseColumns)
//    }
//    else if (isSingleRowObjectArr) {
//      convertor = new DeliveryConvertorObjectArr
//    }
//    else {
//      convertor = new DeliveryConvertorNull
//    }
//    return new ResultDeliveryStrategyImpl(subscriber, convertor, subscriptionMethod, startMethod, endMethod, rStreamMethod)
//  }
//}
//
//
