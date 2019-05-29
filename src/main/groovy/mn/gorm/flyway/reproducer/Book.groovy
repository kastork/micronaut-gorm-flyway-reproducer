package mn.gorm.flyway.reproducer

import grails.gorm.annotation.Entity

@Entity
class Book
{
	String name
	
	static mapping = {
		name sqlType: 'varchar(255)'
	}	
	
	static constraints = {
		name nullable: false, unique: true
	}
}
