package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.i18n.{Messages, I18nSupport, MessagesApi}
import play.api.data.Form
import play.api.data.Forms.{mapping, longNumber, nonEmptyText}
import models.Product

class Products @Inject()(val messagesApi: MessagesApi) extends Controller with I18nSupport{

	def list = Action{ implicit request =>
		val products = Product.findAll

		Ok(views.html.products.list(products))
	}

	def show(ean: Long) = Action{ implicit request =>
		Product.findByEan(ean).map{ product =>
			Ok(views.html.products.details(product))
		}.getOrElse(NotFound)
	}

	def newProduct = Action{ implicit request =>
		val form = if(request.flash.get("error").isDefined)
			productForm.bind(request.flash.data)
		else
			productForm
		Ok(views.html.products.editProduct(form))
	}

	private val productForm: Form[Product] = Form(
		mapping(
			"ean" -> longNumber.verifying(
				"validation.ean.duplicate", Product.findByEan(_).isEmpty),
			"name" -> nonEmptyText,
			"description" -> nonEmptyText
			)(Product.apply)(Product.unapply)
	)

	def save = Action { implicit request => 
		val newProductForm = productForm.bindFromRequest()

		newProductForm.fold(
			hasErrors = {form => 
				Redirect(routes.Products.newProduct()).flashing(Flash(form.data) + 
					("error" -> Messages("validation.errors")))
			},
			success = {newProduct => 
				Product.add(newProduct)
				val message = Messages("products.new.success", newProduct.name)
				Redirect(routes.Products.show(newProduct.ean)).flashing("success" -> message)
			}
		)
	}
}