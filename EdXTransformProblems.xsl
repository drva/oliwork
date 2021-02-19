<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="xs"
    version="2.0">
    <xsl:output
        method="xml" 
        doctype-public="-//Carnegie Mellon University//DTD Inline Assessment 1.4//EN"
        doctype-system="http://oli.web.cmu.edu/dtd/oli_inline_assessment_1_4.dtd"
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
        <xsl:result-document href="{concat('a_',$filename, '.xml')}"> <!--https://www.oxygenxml.com/forum/topic7987.html-->
        <assessment>
            <xsl:attribute name="id"><xsl:value-of select="concat('a_',$filename)"/></xsl:attribute>
            <title> <!--if I understand and remember correctly, this element is necessary but not displayed. However, KTH#2 has some content-bearing display names for problems, which I'd like to put somewhere, and it turns out title is displayed in echo editing so allowing for different ones could be beneficial.-->
                <xsl:choose>
                    <xsl:when test="./@display_name">
                        <xsl:value-of select="./@display_name"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text>tutor</xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
            </title> 
            <question>
                <xsl:attribute name="id"><xsl:value-of select="concat('aQ_', $filename)"/></xsl:attribute> <!--it is not allowed to be identical to the filename-->
                <body>
                    <!--the edx problems have everything in one element while we have body for problem wording etc, so need to seperate them out-->
                    <xsl:apply-templates select="*[not(self::multiplechoiceresponse or self::choiceresponse or self::stringresponse or self::numericalresponse or self::solution or self::demandhint)]"/>
                    
                    <!--kth#2 has a bunch of problems where problem body content is *inside* the relevant question type tag. I need to get it put into body. (this makes the commented out piece below unnecessary as this fulfills the function it previously was, so it is commented out)-->
                    <xsl:apply-templates select="multiplechoiceresponse/node()[not(self::choicegroup or self::solution)]"/>
                    <xsl:apply-templates select="choiceresponse/node()[not(self::checkboxgroup or self::solution)]"/>
                    <xsl:apply-templates select="numericalresponse/node()[not(self::responseparam or self::formulaequationinput or self::additional_answer or self::solution)]"/>
                    <xsl:apply-templates select="stringresponse/node()[not(self::textline or self::correcthint or self::additional_answer or self::stringequalhint or self::solution)]"/>
                    <!--<xsl:apply-templates select = "multiplechoiceresponse/label"/>--> <!--some kth problems have problem body like this-->
                </body>
                <xsl:apply-templates select ="multiplechoiceresponse|choiceresponse|stringresponse|numericalresponse"/>
                <xsl:apply-templates select ="optionresponse" mode="choices"/> <!--bc of how optionresponse is put together differently, it's also dealt with somewhat differently-->
                <xsl:apply-templates select ="optionresponse" mode="feedback"/>
            </question>
            <xsl:apply-templates select="//solution"/>
        </assessment>
        </xsl:result-document>
    </xsl:template>
    <!--when there's more than one question in a problem. Should copy the above but seperate out assessment and question, basically, through q element that I add to seperate out the subproblems-->
    <xsl:template match="problem[q]"> <!--Currently not handling any of the attributes of problem-->
        <xsl:result-document href="{concat('a_',$filename, '.xml')}"> <!--https://www.oxygenxml.com/forum/topic7987.html-->
            <assessment>
                <xsl:attribute name="id"><xsl:value-of select="concat('a_',$filename)"/></xsl:attribute>
                <title> <!--if I understand and remember correctly, this element is necessary but not displayed. However, KTH#2 has some content-bearing display names for problems, which I'd like to put somewhere, and it turns out title is displayed in echo editing so allowing for different ones could be beneficial.-->
                    <xsl:choose>
                        <xsl:when test="./@display_name">
                            <xsl:value-of select="./@display_name"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>tutor</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </title>   
                <xsl:apply-templates/>
            </assessment>
        </xsl:result-document>
    </xsl:template>
    <xsl:template match="q">
        <question>
            <xsl:attribute name="id"><xsl:value-of select="concat('aQ_', $filename, '_',string(count(preceding-sibling::*)))"/></xsl:attribute> <!--it is not allowed to be identical to the filename--> <!--here also need to be numbered, since there's more than one-->
            <body>
                <!--the edx problems have everything in one element while we have body for problem wording etc, so need to seperate them out-->
                <xsl:apply-templates select="*[not(self::multiplechoiceresponse or self::choiceresponse or self::stringresponse or self::numericalresponse or self::solution or self::demandhint)]"/>
                
                <!--kth#2 has a bunch of problems where problem body content is *inside* the relevant question type tag. I need to get it put into body. (this makes the commented out piece below unnecessary as this fulfills the function it previously was, so it is commented out)-->
                <xsl:apply-templates select="multiplechoiceresponse/node()[not(self::choicegroup)]"/>
                <xsl:apply-templates select="choiceresponse/node()[not(self::checkboxgroup)]"/>
                <xsl:apply-templates select="numericalresponse/node()[not(self::responseparam or self::formulaequationinput or self::additional_answer)]"/>
                <xsl:apply-templates select="stringresponse/node()[not(self::textline or self::correcthint or self::additional_answer or self::stringequalhint)]"/>
                <!--<xsl:apply-templates select = "multiplechoiceresponse/label"/>--> <!--some kth problems have problem body like this-->
            </body>
            <xsl:apply-templates select ="multiplechoiceresponse|choiceresponse|stringresponse|numericalresponse"/>
        </question>
    </xsl:template>
    
    <!--<xsl:template match="problem[//optionresponse]"> <!-being treated seperately because it is put together differently->
        <xsl:result-document href="{concat('a_',$filename, '.xml')}">
        <assessment>
            <xsl:attribute name="id"><xsl:value-of select="concat('a_',$filename)"/></xsl:attribute>
            <title> <!-if I understand and remember correctly, this element is necessary but not displayed. However, KTH#2 has some content-bearing display names for problems, which I'd like to put somewhere, and it turns out title is displayed in echo editing so allowing for different ones could be beneficial.->
                <xsl:choose>
                    <xsl:when test="./@display_name">
                        <xsl:value-of select="./@display_name"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text>tutor</xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
            </title> 
            <question>
                <xsl:attribute name="id"><xsl:value-of select="concat('aQ_', $filename)"/></xsl:attribute> <!-it is not allowed to be identical to the filename->
                <body>
                    <xsl:apply-templates/>
                </body>
                <xsl:apply-templates select ="optionresponse" mode="choices"/>
                <xsl:apply-templates select ="optionresponse" mode="feedback"/>
            </question>    
        </assessment>
        </xsl:result-document>
    </xsl:template>-->
    
    <xsl:template match="submit_and_compare"> <!--Currently not handling any of the other attributes  here-->
        <assessment>
            <xsl:attribute name="id"><xsl:value-of select="concat('a_',./@url_name)"/></xsl:attribute>
            <title>tutor</title> <!--if I understand and remember correctly, this element is necessary but not displayed-->
            <question>
                <xsl:attribute name="id"><xsl:value-of select="concat('aQ_', ./@url_name)"/></xsl:attribute> <!--it is not allowed to be identical to the filename-->
                <xsl:apply-templates select="*[not(self::demandhint)]"/> <!--their hints are after their explanation section directly in the root and ours are inside our part element, so need to get them in the right place-->
            </question>    
        </assessment>
    </xsl:template>
    <!--body is currently being handled by the identity transformation-->
    <xsl:template match="explanation">
        <short_answer id="answer" case_sensitive="false" /> <!--not sure if the id/input thing is required (looking at Diana's course) but putting it in in case-->
        <part>
            <response match="*" input="answer" score="1">
                <feedback>
                    <xsl:apply-templates/>
                </feedback>
            </response>
            <xsl:apply-templates select="//demandhint"/>
        </part>
    </xsl:template>
    <xsl:template match="demandhint">
        <xsl:apply-templates/>
    </xsl:template>
    <!--hint is currently being handled by the identity transformation-->
    
<!--various problem text elements--> 
    <!--CHECK Do we have a way to do a title?-->
    <xsl:template match="h2|h3">
        <em><xsl:apply-templates/></em> <!--this had the version that pulls in attributes before but I don't think I want that?-->
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
    
    <!--for problems that have feedback in python. Note: this will end up in body due to how I structures the above, but that should be fine since it'll be gotten rid of ultimately anyway-->
    <xsl:template match="script"> <!--currently just putting in comments-->
        <xsl:text disable-output-escaping="yes">
            &lt;!--</xsl:text>
        <script>
            <xsl:apply-templates select="@* | node()"/>   
        </script>--<xsl:text disable-output-escaping="yes">&gt;
        </xsl:text> 
    </xsl:template>
    
<!--Problem types-->
    <!--mutiple choice-->
    <xsl:template match="multiplechoiceresponse">
        <xsl:apply-templates select="choicegroup"/> <!--kth#2 has a bunch of problems where problem body content is *inside* the relevant question type tag, and that was processed in body and should not be included here. This replaces the below.-->
        <!--<xsl:apply-templates select="*[not(self::label)]"/>--> <!--just sending it through to be dealt with in the next one--> <!--excluding label, from kth, since contents of that go in body-->
    </xsl:template>
    <xsl:template match="multiplechoiceresponse/choicegroup"> <!--at the moment it looks like these always come together like this. Also choicegroup can have a type attribute (its value tends to be MultipleChoice) but it doesn't look to be doing anything? If either of these are false will need to change.-->
        <multiple_choice shuffle="false"> <!--it doesn't look like they have shuffle-->
            <xsl:apply-templates/>
        </multiple_choice>
        <!--edX has the problems and the feedback together, but we don't, so need to seperate them out-->
        <part>
            <xsl:apply-templates select="choice" mode="feedback"/>       
            <xsl:apply-templates select="//demandhint"/> <!--kth has hints in mcs-->
        </part>
    </xsl:template>
    
    <xsl:template match="choice"> <!--this one is for the problem (as opposed to feedback)--> 
        <!--we need value attributes to identify our choices, which don't exist in the edx source. Doing this by numbering them in order-->
        <choice value="{./count(preceding-sibling::*)}"><xsl:apply-templates select="(text() | *[not(self::choicehint)])"/></choice> <!--choicehint goes in feedback, not here-->
    </xsl:template>
    
    <!--handling correct and incorrect answers--> <!--since multiple select also has choice tags but the feedback needs to work differently, specifying this is for multiple choice only-->
    <xsl:template match="multiplechoiceresponse/choicegroup/choice[@correct='true']" mode="feedback">
        <xsl:variable name="choiceid" select="./count(preceding-sibling::*)"/>
        <response match="{$choiceid}" score="{$points}">
            <feedback>
                <xsl:text>Correct! </xsl:text> <!--edx has this seperately from the feedback, so adding it in. Also handles cases without feedback.-->
                <!--Philanthropy has feedback in choicehint elements, while Course Design has it in Python scripts-->
                <xsl:apply-templates select="choicehint"/>
                <xsl:apply-templates select="//script" mode="mcfeedback">
                    <xsl:with-param name="choiceidparam" select="$choiceid"/> <!--will need to know which choice's feedback I want-->
                </xsl:apply-templates>
            </feedback>
        </response>
    </xsl:template>
    <xsl:template match="multiplechoiceresponse/choicegroup/choice[@correct='false']" mode="feedback">
        <xsl:variable name="choiceid" select="./count(preceding-sibling::*)"/>
        <response match="{$choiceid}" score="0">
            <feedback>
                <xsl:text>Incorrect. </xsl:text>
                <xsl:apply-templates select="choicehint"/>
                <xsl:apply-templates select="//script" mode="mcfeedback">
                    <xsl:with-param name="choiceidparam" select="$choiceid"/>
                </xsl:apply-templates>
            </feedback>
        </response>
    </xsl:template>
    
    <xsl:template match="choicehint">       
            <xsl:apply-templates/>     
    </xsl:template>
    
    <!--using regular expressions to pull feedback out of the python scripts. Differs for multiple choice and multiple select-->
    <xsl:template match="script" mode="mcfeedback">
        <xsl:param name="choiceidparam"/>
        <xsl:analyze-string select="."
            regex="[\s\S]*?{$choiceidparam}'\s+in\s+ans:\s*feedback\s*=\s*'([\s\S]+?)'[\s\S]*">
            <!--see for example
            elif 'choice_1' in ans:
                feedback = 'Not quite right.-->
    
            <xsl:matching-substring>
                <xsl:value-of select="regex-group(1)"/>
            </xsl:matching-substring>
            <xsl:non-matching-substring/> <!--I'm not sure if I need this in here to say that if it doesn't find a match it shouldn't output anything?-->
        </xsl:analyze-string> <!--https://www.xml.com/pub/a/2003/06/04/tr.html-->
    </xsl:template>
    
    
    <!--multiple select-->
        <!--note: the list of choices here works the same as in multiple choice and can be handled by the same template (above). Feedback, however, works differently, and needs its own handling-->
    <xsl:template match="choiceresponse">
        <xsl:apply-templates select="checkboxgroup"/> <!--kth#2 has a bunch of problems where problem body content is *inside* the relevant question type tag, and that was processed in body and should not be included here. This replaces the below.-->
        <!--<xsl:apply-templates/>--> <!--just sending it through to be dealt with in the next one-->
    </xsl:template>
    <xsl:template match="choiceresponse/checkboxgroup"> <!--at the moment it looks like these always come together like this. Also checkboxgroup can have a label attribute and a direction attribute (in course design they seem to tend to say 'x' and 'vertical') but they don't look to be doing anything? If either of these are false will need to change.-->
        <multiple_choice select="multiple" shuffle="false"> <!--it doesn't look like they have shuffle-->
            <xsl:apply-templates select="*[not(self::compoundhint)]"/> <!--compoundhint, seen in kth, goes with feedback-->
        </multiple_choice>
        <!--edX has the problems and the feedback together, but we don't, so need to seperate them out-->
            <!--the minimum needed for multiple select feedback is 'selecting all and only the answers that should be selected is correct; everything else is wrong'-->
        <xsl:variable name="correctmatch" select="string-join((choice[@correct='true']/string(count(preceding-sibling::*))), ',')"/> <!--preceding siblings is again used to number and this identify the choices-->
        <part>
            <xsl:choose>
                <xsl:when test="choice/choicehint">
                    <xsl:apply-templates select="choice" mode="feedback"/> <!--this is needed when the feedback, that being choicehint, exists-->
                </xsl:when>
            </xsl:choose>
            <xsl:apply-templates select="compoundhint"/>
            <!--^based on kth-->
            <response  match="{$correctmatch}" score="1">              
                <feedback>
                    Correct.
                </feedback>
            </response>
            <response match="*"  score="0">
                <feedback>
                    Incorrect.
                </feedback>
            </response>
        </part>
    </xsl:template>
    <xsl:template match="choiceresponse/checkboxgroup/choice" mode="feedback">
        <xsl:variable name="choiceid" select="./count(preceding-sibling::*)"/>
        <response match="{$choiceid}" score="0">
            <feedback>
                <xsl:apply-templates select="choicehint"/>
            </feedback>
        </response>
    </xsl:template>
    <xsl:template match="compoundhint"> <!--note: atm this is set up to need handfinishing after, bc it's not changing the choice ids here to ours, and it's not marking the correct answer (but the basic 'this is the correct answer' will end up in the file so it can be combined by hand with the proper feedback)-->
        <response  match="{@value}" score="0">              
            <feedback>
                <xsl:apply-templates/>
            </feedback>
        </response>
    </xsl:template>
    
    
    <!--optionresponse-->
    <xsl:template match="optionresponse"> <!--this is the one in body, so it just has the input_ref--> <!--I am ignoring the label attribute atm (it is sometimes used clearly unintentionally)-->
        <input_ref input="{concat('input',./count(preceding::optionresponse))}"/> <!--numbering for ids-->
    </xsl:template>
    
        <!--looking at kth#2-->
    <xsl:template match="optionresponse" mode="choices"> <!--these are the ones for the list of inputs and their choices that's below body-->
        <fill_in_the_blank id="{concat('input',./count(preceding::optionresponse))}">
          <xsl:apply-templates/>
        </fill_in_the_blank>    
    </xsl:template>
    <xsl:template match="optioninput"><xsl:apply-templates/></xsl:template> <!--sending through-->
    <xsl:template match="optioninput/option">
        <choice value="{./count(preceding-sibling::*)}"><xsl:apply-templates/></choice> <!--doing ids by numbering-->
    </xsl:template>
    
    <xsl:template match="optionresponse" mode="feedback"> <!--the feedback ones-->
        <part>
           <xsl:apply-templates mode="feedback"/> 
        </part>  
    </xsl:template>
    <xsl:template match="optioninput" mode="feedback"><xsl:apply-templates mode="feedback"/></xsl:template> <!--sending through-->
    <xsl:template match="optioninput/option[@correct='true' or @correct='True']" mode="feedback">
        <response input="{concat('input', ./parent::*/parent::*/count(preceding::optionresponse))}" match="{./count(preceding-sibling::*)}" score="{$points}"> <!--getting the correct corresponding ids etc for the input and choice this goes with-->
            <feedback>
                <xsl:text>Correct! </xsl:text> <!--edx has this seperately from the feedback, so adding it in. Also handles cases without feedback.-->
            </feedback>
        </response>
    </xsl:template>
    <xsl:template match="optioninput/option[@correct='false' or @correct='False']" mode="feedback">
        <response input="{concat('input', ./parent::*/parent::*/count(preceding::optionresponse))}" match="{./count(preceding-sibling::*)}" score="0"> <!--getting the correct corresponding ids etc for the input and choice this goes with-->
            <feedback>
                <xsl:text>Incorrect. </xsl:text> <!--edx has this seperately from the feedback, so adding it in. Also handles cases without feedback.-->
            </feedback>
        </response>
    </xsl:template>
    
    
    <!--string response (looking at kth)-->
    <xsl:template match="stringresponse">
        <text name="field"  size="medium" whitespace="trim" case_sensitive="false"/> <!--making these the same for all atm-->
        <part>
            <response score="1">
                <xsl:attribute name="match">
                    <xsl:choose>
                        <xsl:when test="contains(@type,'regexp')">
                            <xsl:value-of select="concat('/',@answer,'/')"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="./@answer"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:attribute>
                <feedback>
                    <xsl:apply-templates select="correcthint"/>
                </feedback>
            </response>
            <xsl:apply-templates select="additional_answer"/> <!--I *think* these are other correct answers?-->
            <xsl:apply-templates select="stringequalhint"/> <!--these seem to be specific wrong answers-->
            <response match="*">
                <feedback>
                    Try Again.
                </feedback>
            </response>
            <xsl:apply-templates select="//demandhint"/>
        </part>
    </xsl:template>
    <xsl:template match="correcthint">
        <xsl:apply-templates/>
    </xsl:template>
    <xsl:template match="additional_answer">
        <response score="1">
            <xsl:attribute name="match">
                <xsl:value-of select="./@answer"/>
            </xsl:attribute>
            <feedback>
                <xsl:text>Correct! </xsl:text><xsl:apply-templates/>
            </feedback>
        </response>
    </xsl:template>
    <xsl:template match="stringequalhint">
        <response score="0">
            <xsl:attribute name="match">
                <xsl:value-of select="./@answer"/>
            </xsl:attribute>
            <feedback>
                <xsl:apply-templates/>
            </feedback>
        </response>
    </xsl:template>
    
    <!--numeric (looking at kth#2)-->
    <xsl:template match="numericalresponse">
        <numeric id="field"/> <!--don't think this id matters for anything, looking at diana's course examples?-->
        <part>
            <response score="1" match="{./@answer}"> <!--diana's example put exact answer and range seperately and that seems like a good idea even if they have the same point value-->
                <feedback>Correct!</feedback>
            </response> 
            <xsl:choose> <!--if there's an acceptable range, need to accept answers within it-->
                <xsl:when test="./responseparam[@type='tolerance']">
                    <xsl:comment><xsl:text>tolerance: </xsl:text><xsl:value-of select="./responseparam/@default"/></xsl:comment>
                    <response score="1">
                        <xsl:attribute name="match">
                            <xsl:choose><!--sometimes the tolerance is a number and sometimes is a percent; handling both-->
                                <xsl:when test="./responseparam[contains(@default, '%')]">
                                    <xsl:variable name="percentvalue" select="substring-before(./responseparam/@default,'%')"/>
                                    <xsl:value-of select="concat('[',string((1-.01*number($percentvalue))*number(./@answer)),',',string((1+.01*number($percentvalue))*number(./@answer)),']')"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="concat('[',string(number(./@answer)-number(./responseparam/@default)),',',string(number(./@answer)+number(./responseparam/@default)),']')"/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:attribute>
                        <feedback>Correct!</feedback>
                    </response>
                </xsl:when>
            </xsl:choose>
            <xsl:apply-templates select="additional_answer"/> <!--I *think* these are other correct answers?-->
            <response match="*">
                <feedback>Incorrect.</feedback>
            </response>
        </part>
    </xsl:template>
    
<!--'solution'-->
    <!--looking at kth#3/ramp i-->
    <!--edX has a solution/detailed solution thing, seperate from other kinds of feedback. Currently deciding to handle it by making it its own question.
    This keeps it distinct from feedback, if applicable, and allows the student to try multiple attempts without seeing it but still definitely be able to see it if they want.-->
    <xsl:template match="solution">
        <question id="{concat('solution', ./count(preceding-sibling::*))}"> <!--originally it was just solution, but ran into a problem with more than one. Doing preceding sibling bc it's simple, will def not be the same, and I don't actually need the number to mean anything-->
            <body><em style="bold">Detailed solution</em></body>
            <multiple_choice shuffle="false">
                <choice value="0">Click here to see the detailed solution.</choice>
            </multiple_choice>
            <part>
                <response match="0" score="0">
                    <feedback><xsl:apply-templates/></feedback>
                </response>
            </part>
        </question>
    </xsl:template>
    
<!--hints-->
    <!--At least some feedback-in-python files have these. They can go with a hint in the python, according to the docs, but there are files with this but no actual hint.
    Not getting completely rid of it atm in case some files *do* have hints, so currently commenting out.-->       
    <xsl:template match="hintgroup[@hintfn='hint_fn']"> 
        <xsl:text disable-output-escaping="yes">&lt;!--</xsl:text>
        <hintgroup>
            <xsl:apply-templates select="@* | node()"/>   
        </hintgroup>--<xsl:text disable-output-escaping="yes">&gt;</xsl:text> 
        <!--I feel like there should be a way to do this that doesn't involve retyping the tag, but don't know what it is-->
    </xsl:template>
    
 <!--for KTH-->
    <xsl:template match="label">
        <p><xsl:apply-templates/></p>
    </xsl:template>
    
    <xsl:template match="pre">
        <code><xsl:apply-templates/></code>
    </xsl:template>
 
<!--for KTH#2-->
    <xsl:template match="pre[code]">
        <codeblock syntax="text">
            <xsl:apply-templates select="./code/node()"/>
        </codeblock>
    </xsl:template>
    
    <xsl:template match="description">
        <p><xsl:apply-templates/></p>
    </xsl:template>
        
    <xsl:template match="span"> <!--just stripping out atm-->
        <xsl:apply-templates/>
    </xsl:template>
<!--for KTH#3/ramp i--> 
    <xsl:template match="div"> <!--just stripping out atm-->
        <xsl:apply-templates/>
    </xsl:template>
    
    <!--attempting to do: br's in paragraphs should be stripped out (not sure what else to do) but noted; brs not in paragraphs should get an empty p-->
    <xsl:template match="p/br">
        <xsl:comment><br/></xsl:comment>
    </xsl:template>
    <xsl:template match="br">
        <p/>
    </xsl:template>
    
    <!--library_content-->
        <!--library_content is meant to be pools, but we don't have pools for inline assessments. Here all the problems are kept in, but rather than making the library_content an assessment, it's made a section with links to the problems-->
    <xsl:template match="library_content"> <!--putting this here rather than making a new stylesheet since it should be short and is relevant to problems. This means I'll still have the header, which this doesn't need, but can strip out later-->
        <xsl:result-document href="{concat('lc_',$filename, '.xml')}">
            <section>
                <title><xsl:value-of select="./@display_name"/></title> <!--ignoring some other attributes-->
                <body>
                    <xsl:apply-templates/>
                </body>
            </section>
        </xsl:result-document>
    </xsl:template>
    <xsl:template match="library_content/problem">
        <inline idref="{concat('a_',./@url_name)}"/> <!--this should be wb:inline but that causes namespace problems and since I will need an extra script anyway that will be handled there-->
    </xsl:template>
</xsl:stylesheet>