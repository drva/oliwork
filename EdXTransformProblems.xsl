<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="xs"
    version="2.0">
    <xsl:output
        method="xml" 
        doctype-public="-//Carnegie Mellon University//DTD Inline Assessment 1.3//EN"
        doctype-system="http://oli.web.cmu.edu/dtd/oli_inline_assessment_1_3.dtd"
        indent="yes"/>
    
    <!--I want to use the existing filename for the id-->
    <xsl:variable name="document-uri" select="document-uri(.)"/>
    <xsl:variable name="filename" select="(tokenize($document-uri,'[/.]'))[last()-1]"/>
    
    <!--basic value of how many points to give for correct answers-->
    <xsl:variable name="points" select="1"/>
    
    <!--I don't want to miss anything, so adding the identity transformation to by default copy everything
        (and be overidden by more specific templates for things that need different handling)-->
    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="problem"> <!--Currently not handling any of the attributes of problem-->
        <assessment>
            <xsl:attribute name="id"><xsl:value-of select="concat('a_',$filename)"/></xsl:attribute>
            <title>tutor</title> <!--if I understand and remember correctly, this element is necessary but not displayed-->
            <question>
                <xsl:attribute name="id"><xsl:value-of select="concat('aQ_', $filename)"/></xsl:attribute> <!--it is not allowed to be identical to the filename-->
                <body>
                    <!--the edx problems have everything in one element while we have body for problem wording etc, so need to seperate them out-->
                    <xsl:apply-templates select="*[not(self::multiplechoiceresponse or self::optionresponse or self::choiceresponse or self::numericalresponse or self::solution)]"/>
                </body>
                <xsl:apply-templates select ="multiplechoiceresponse|optionresponse|choiceresponse|numericalresponse|solution"/>
            </question>    
        </assessment>
    </xsl:template>
    
<!--various problem text elements--> 
    <!--CHECK Do we have a way to do a title?-->
    <xsl:template match="h2">
        <em><xsl:apply-templates select="@* | node()"/></em>
    </xsl:template>
    
    <!--bold and italics--> <!--copied from the workbook page stylesheet-->
    <xsl:template match="strong | b">
        <em style="bold"><xsl:apply-templates select="@* | node()"/></em>
    </xsl:template>
    <xsl:template match="i | em"> <!--POSSIBLY CHECK note, we also allow a plain em tag but it looks like it produces bold italics and html em is usually italics?-->
        <em style="italic"><xsl:apply-templates select="@* | node()"/></em>
    </xsl:template>
    
    <xsl:template match="a">
        <link>
            <xsl:apply-templates select="@href | @title | node()"/> 
        </link>
    </xsl:template>
    <xsl:template match="img">
        <image>
            <xsl:attribute name="src"><xsl:value-of select="concat('..',replace(./@src,'static','webcontent'))"/></xsl:attribute>
            <xsl:apply-templates select="@alt | @title | @height | @width | node()"/> 
        </image>
    </xsl:template>
    <!--NOTE, p should currently be handled by the identity transformation, if I get rid of that I'll need a replacement-->
    
    <!--Problem types-->
    <xsl:template match="multiplechoiceresponse">
        <xsl:apply-templates/> <!--just sending it through to be dealt with in the next one-->
    </xsl:template>
    <xsl:template match="multiplechoiceresponse/choicegroup"> <!--at the moment it looks like these always come together like this. Also choicegroup can have a type attribute (its value tends to be MultipleChoice) but it doesn't look to be doing anything? If either of these are false will need to change.-->
        <multiple_choice shuffle="false"> <!--it doesn't look like they have shuffle-->
            <xsl:apply-templates/>
        </multiple_choice>
        <!--edX has the problems and the feedback together, but we don't, so need to seperate them out-->
        <part>
            <xsl:apply-templates select="choice" mode="feedback"/>
        </part>
    </xsl:template>
    
    <xsl:template match="choice"> <!--this one is for the problem (as opposed to feedback)--> 
        <!--we need value attributes to identify our choices, which don't exist in the edx source. Doing this by numbering them in order-->
        <choice value="{./count(preceding-sibling::*)}"><xsl:apply-templates select="(text() | *[not(self::choicehint)])"/></choice> <!--choicehint goes in feedback, not here-->
    </xsl:template>
    
    <!--handling correct and incorrect answers-->
    <xsl:template match="choice[@correct='true']" mode="feedback">
        <response match="{./count(preceding-sibling::*)}" score="{$points}">
            <xsl:apply-templates select="choicehint"/>
        </response>
    </xsl:template>
    <xsl:template match="choice[@correct='false']" mode="feedback">
        <response match="{./count(preceding-sibling::*)}" score="0">
            <xsl:apply-templates select="choicehint"/>
        </response>
    </xsl:template>
    
    <xsl:template match="choicehint">
        <feedback>
            <xsl:apply-templates/>
        </feedback>
    </xsl:template>
</xsl:stylesheet>