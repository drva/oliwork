<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="xs"
    version="2.0">
    <xsl:output
        method="xml" 
        doctype-public="-//Carnegie Mellon University//DTD Learning Objectives 2.0//EN"
        doctype-system="http://oli.web.cmu.edu/dtd/oli_learning_objectives_2_0.dtd"
        indent="yes"/>
    
    <!--identity transformation to by default copy everything
        (and be overidden by more specific templates for things that need different handling)-->
    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>
    
    <!--bold and italics-->
    <xsl:template match="strong | b">
        <em style="bold"><xsl:apply-templates select="@* | node()"/></em>
    </xsl:template>
    <xsl:template match="i | em"> <!--POSSIBLY CHECK note, we also allow a plain em tag but it looks like it produces bold italics and html em is usually italics?-->
        <em style="italic"><xsl:apply-templates select="@* | node()"/></em>
    </xsl:template>
    
    <!--the philanthropy course had a different format for LOs, which was dealt with differently in the java and needs finishing dealing with here-->
    <xsl:template match="h3[@class='oli_objective']">
        <xsl:apply-templates/> 
    </xsl:template>
    <!--the other thing to deal with there is the empty <p>'s. LEAVING FOR LATER-->
    
</xsl:stylesheet>