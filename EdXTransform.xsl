<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="xs"
    version="2.0">
    <xsl:output
        method="xml" 
        doctype-public="-//Carnegie Mellon University//DTD Workbook Page 3.8//EN"
        doctype-system="http://oli.web.cmu.edu/dtd/oli_workbook_page_3_8.dtd"
        indent="yes"/>
    
    <!--Setting up to find the LO file-->
    <xsl:variable name="fileid" select="workbook_page/@id"/>
    <xsl:variable name="lofile" select="concat('LOs_',$fileid,'.xml')"/>
    
    <!--I don't want to miss anything, so adding the identity transformation to by default copy everything
        (and be overidden by more specific templates for things that need different handling)-->
    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>
    
    <!--pulling in the learning objectives from their file. 
        Note, atm LO file needs to be in the same directory as the xsl document.
        https://www.xml.com/pub/a/2002/03/06/xslt.html-->
    <xsl:template match="workbook_page/head">
        <head>
            <xsl:apply-templates select="@* | node()"/>
            <xsl:if test="document($lofile)">
                <xsl:apply-templates select="document($lofile)/objectives/objective"/>
            </xsl:if>  
        </head>
    </xsl:template>
    <xsl:template match="objective">
        <objref>
            <xsl:attribute name="idref">
                <xsl:value-of select="./@id"/>
            </xsl:attribute>
        </objref>
    </xsl:template>
    
    <!--were-h1s will be put in comments - don't want them in the output because the page title was already generated from display name, 
        but want them available because the two don't always match and this way they are easily available to compare-->
    <xsl:template match="h1">
        <xsl:text disable-output-escaping="yes">&lt;!--</xsl:text><h1><xsl:value-of select="."/></h1>--<xsl:text disable-output-escaping="yes">&gt;</xsl:text> <!--Trying to figure out how to do this. &lt;!- etc generates that same thing in output, which I don't want. From https://stackoverflow.com/questions/3932152/output-element-in-comments tried the xsl:comment thing but that gets rid of the h1 tags. From same source found this, which works!--> 
    </xsl:template>
    <!--generated-from-htmls-sections headed by h1s should actually not be sections, since the h1 is just the page title, not a section on the page-->
    <xsl:template match="section[h1]">
        <xsl:apply-templates select="@* | node()"/>
    </xsl:template>
    
    <!--sections were already generated so just need to change headers to the titles of those sections-->
    <xsl:template match="h2|h3|h4|h5|h6">
        <title><xsl:value-of select="."/></title>
    </xsl:template>
    <!--and put in bodies after the titles-->
    <xsl:template match="section[h2|h3|h4|h5|h6]">
        <section>
            <xsl:apply-templates select="h2|h3|h4|h5|h6"/>
            <body>
                <xsl:apply-templates select="*[not(self::h2 or self::h3 or self::h4 or self::h5 or self::h6)]"/>
            </body>
        </section> 
    </xsl:template>
    
    <!--don't think we need these since we do our own styling-->
    <xsl:template match="link[@rel='stylesheet']"></xsl:template>
    <!--Get rid of the LO section-->
    <xsl:template match="div[descendant::li[matches(text(),'LO WAS HERE')]]"></xsl:template>
    
    <xsl:template match="strong">
        <em style="bold"><xsl:apply-templates select="@* | node()"/></em>
    </xsl:template>
    <xsl:template match="i">
        <em style="italic"><xsl:apply-templates select="@* | node()"/></em>
    </xsl:template>
    
</xsl:stylesheet>