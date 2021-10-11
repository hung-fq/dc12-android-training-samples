#Repository URL:
https://github.com/hung-fq/dc12-android-training-samples.git

#Rules:
1. Create new child-branch from develop for working on task with below template:
	yourname-appname (Ex: hung-tcalculator)
	(Will use ticket id when main project is started)

2. Commit rule:
	Always do Pull before commit/push src code
	Message: [branch-name] Message contents...

3. Review:
	After done code for a task, request sub-leader for reviewing
	iOS: VuongLy
	android: HungPham

4. Merge branch:
	Sub-Leader will merge child-branch to develop after review/rework
	   Message: Merge pull request #XX from original-branch to destination-branch
	   Ex: Merge pull request #1 from hung-tcalculator to develop
    
	Also delete child-branch after merged

5. Release:
	Default to release 1time/week (Fri 11:30 PM)
	Sub-Leader will merge from develop to master branch then create tag.
	Tag name: yyyyMMdd_verXYZ
